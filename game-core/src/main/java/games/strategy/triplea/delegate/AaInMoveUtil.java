package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.triplea.java.collections.CollectionUtils;

import com.google.common.collect.ImmutableList;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleAPlayer;

/**
 * Code to fire AA guns while in combat and non combat move.
 */
class AaInMoveUtil implements Serializable {
  private static final long serialVersionUID = 1787497998642717678L;

  private transient IDelegateBridge bridge;
  private transient PlayerId player;
  private Collection<Unit> casualties = new ArrayList<>();
  private final ExecutionStack executionStack = new ExecutionStack();

  AaInMoveUtil() {}

  public void initialize(final IDelegateBridge bridge) {
    this.bridge = bridge;
    this.player = bridge.getPlayerId();
  }

  private GameData getData() {
    return bridge.getData();
  }

  private boolean isAlwaysOnAaEnabled() {
    return Properties.getAlwaysOnAa(getData());
  }

  private boolean isAaTerritoryRestricted() {
    return Properties.getAaTerritoryRestricted(getData());
  }

  private ITripleAPlayer getRemotePlayer(final PlayerId id) {
    return (ITripleAPlayer) bridge.getRemotePlayer(id);
  }

  private ITripleAPlayer getRemotePlayer() {
    return getRemotePlayer(player);
  }

  /**
   * Fire aa guns. Returns units to remove.
   */
  Collection<Unit> fireAa(final Route route, final Collection<Unit> units, final Comparator<Unit> decreasingMovement,
      final UndoableMove currentMove) {
    if (executionStack.isEmpty()) {
      populateExecutionStack(route, units, decreasingMovement, currentMove);
    }
    executionStack.execute(bridge);
    return casualties;
  }

  /**
   * Fire the aa units in the given territory, hits are removed from units.
   */
  private void fireAa(final Territory territory, final Collection<Unit> units, final UndoableMove currentMove) {
    if (units.isEmpty()) {
      return;
    }
    final PlayerId movingPlayer = movingPlayer(units);
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAa(movingPlayer, getData());
    final List<Unit> defendingAa = territory.getUnitCollection().getMatches(Matches.unitIsAaThatCanFire(units,
        airborneTechTargetsAllowed, movingPlayer, Matches.unitIsAaForFlyOverOnly(), 1, true, getData()));
    // comes ordered alphabetically already
    final List<String> aaTypes = UnitAttachment.getAllOfTypeAas(defendingAa);
    // stacks are backwards
    Collections.reverse(aaTypes);
    for (final String currentTypeAa : aaTypes) {
      final Collection<Unit> currentPossibleAa =
          CollectionUtils.getMatches(defendingAa, Matches.unitIsAaOfTypeAa(currentTypeAa));
      final Set<UnitType> targetUnitTypesForThisTypeAa =
          UnitAttachment.get(currentPossibleAa.iterator().next().getType()).getTargetsAa(getData());
      final Set<UnitType> airborneTypesTargettedToo = airborneTechTargetsAllowed.get(currentTypeAa);
      final Collection<Unit> validTargetedUnitsForThisRoll =
          CollectionUtils.getMatches(units, Matches.unitIsOfTypes(targetUnitTypesForThisTypeAa)
              .or(Matches.unitIsAirborne().and(Matches.unitIsOfTypes(airborneTypesTargettedToo))));
      // once we fire the AA guns, we can't undo
      // otherwise you could keep undoing and redoing until you got the roll you wanted
      currentMove.setCantUndo("Move cannot be undone after " + currentTypeAa + " has fired.");
      final DiceRoll[] dice = new DiceRoll[1];
      final IExecutable rollDice = new IExecutable() {
        private static final long serialVersionUID = 4714364489659654758L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          // get rid of units already killed, so we don't target them twice
          validTargetedUnitsForThisRoll.removeAll(casualties);
          if (!validTargetedUnitsForThisRoll.isEmpty()) {
            dice[0] = DiceRoll.rollSbrOrFlyOverAa(validTargetedUnitsForThisRoll, currentPossibleAa,
                AaInMoveUtil.this.bridge, territory, true);
          }
        }
      };
      final IExecutable selectCasualties = new IExecutable() {
        private static final long serialVersionUID = -8633263235214834617L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          if (!validTargetedUnitsForThisRoll.isEmpty()) {
            final int hitCount = dice[0].getHits();
            if (hitCount == 0) {
              if (currentTypeAa.equals("AA")) {
                AaInMoveUtil.this.bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AA_MISS,
                    findDefender(currentPossibleAa, territory));
              } else {
                AaInMoveUtil.this.bridge.getSoundChannelBroadcaster().playSoundForAll(
                    SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAa.toLowerCase() + SoundPath.CLIP_BATTLE_X_MISS,
                    findDefender(currentPossibleAa, territory));
              }
              getRemotePlayer().reportMessage("No " + currentTypeAa + " hits in " + territory.getName(),
                  "No " + currentTypeAa + " hits in " + territory.getName());
            } else {
              if (currentTypeAa.equals("AA")) {
                AaInMoveUtil.this.bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AA_HIT,
                    findDefender(currentPossibleAa, territory));
              } else {
                AaInMoveUtil.this.bridge.getSoundChannelBroadcaster().playSoundForAll(
                    SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAa.toLowerCase() + SoundPath.CLIP_BATTLE_X_HIT,
                    findDefender(currentPossibleAa, territory));
              }
              selectCasualties(dice[0], units, validTargetedUnitsForThisRoll, currentPossibleAa, defendingAa, territory,
                  currentTypeAa);
            }
          }
        }
      };
      // push in reverse order of execution
      executionStack.push(selectCasualties);
      executionStack.push(rollDice);
    }
  }

  private void populateExecutionStack(final Route route, final Collection<Unit> units,
      final Comparator<Unit> decreasingMovement, final UndoableMove currentMove) {
    final List<Unit> targets = new ArrayList<>(units);
    // select units with lowest movement first
    targets.sort(decreasingMovement);
    final List<IExecutable> executables = new ArrayList<>();
    for (final Territory location : getTerritoriesWhereAaWillFire(route, units)) {
      executables.add(new IExecutable() {
        private static final long serialVersionUID = -1545771595683434276L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          fireAa(location, targets, currentMove);
        }
      });
    }
    Collections.reverse(executables);
    executionStack.push(executables);
  }

  Collection<Territory> getTerritoriesWhereAaWillFire(final Route route, final Collection<Unit> units) {
    final boolean alwaysOnAa = isAlwaysOnAaEnabled();
    // Just the attacked territory will have AA firing
    if (!alwaysOnAa && isAaTerritoryRestricted()) {
      return Collections.emptyList();
    }
    final GameData data = getData();
    // No AA in nonCombat unless 'Always on AA'
    if (GameStepPropertiesHelper.isNonCombatMove(data, false) && !alwaysOnAa) {
      return Collections.emptyList();
    }
    // can't rely on player being the unit owner in Edit Mode
    // look at the units being moved to determine allies and enemies
    final PlayerId movingPlayer = movingPlayer(units);
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAa(movingPlayer, data);
    // don't iterate over the end
    // that will be a battle and handled else where in this tangled mess
    final Predicate<Unit> hasAa = Matches.unitIsAaThatCanFire(units, airborneTechTargetsAllowed, movingPlayer,
        Matches.unitIsAaForFlyOverOnly(), 1, true, data);
    // AA guns in transports shouldn't be able to fire
    final List<Territory> territoriesWhereAaWillFire = new ArrayList<>();
    for (final Territory current : route.getMiddleSteps()) {
      if (current.getUnitCollection().anyMatch(hasAa)) {
        territoriesWhereAaWillFire.add(current);
      }
    }
    if (Properties.getForceAaAttacksForLastStepOfFlyOver(data)) {
      if (route.getEnd().getUnitCollection().anyMatch(hasAa)) {
        territoriesWhereAaWillFire.add(route.getEnd());
      }
    } else {
      // Since we are not firing on the last step, check the start as well, to prevent the user from moving to and from
      // AA sites one at a time
      // if there was a battle fought there then don't fire, this covers the case where we fight, and "Always On AA"
      // wants to fire after the battle.
      // TODO: there is a bug in which if you move an air unit to a battle site in the middle of non combat, it wont
      // fire
      if (route.getStart().getUnitCollection().anyMatch(hasAa)
          && !getBattleTracker().wasBattleFought(route.getStart())) {
        territoriesWhereAaWillFire.add(route.getStart());
      }
    }
    return ImmutableList.copyOf(territoriesWhereAaWillFire);
  }

  private BattleTracker getBattleTracker() {
    return DelegateFinder.battleDelegate(getData()).getBattleTracker();
  }

  private PlayerId movingPlayer(final Collection<Unit> units) {
    if (units.stream().anyMatch(Matches.unitIsOwnedBy(player))) {
      return player;
    }

    return units.stream()
        .filter(Objects::nonNull)
        .map(Unit::getOwner)
        .filter(Objects::nonNull)
        .findAny()
        .orElse(PlayerId.NULL_PLAYERID);
  }

  private static PlayerId findDefender(final Collection<Unit> defendingUnits, final Territory territory) {
    if (defendingUnits == null || defendingUnits.isEmpty()) {
      if (territory != null && territory.getOwner() != null && !territory.getOwner().isNull()) {
        return territory.getOwner();
      }
      return PlayerId.NULL_PLAYERID;
    } else if (territory != null && territory.getOwner() != null && !territory.getOwner().isNull()
        && defendingUnits.stream().anyMatch(Matches.unitIsOwnedBy(territory.getOwner()))) {
      return territory.getOwner();
    }
    return defendingUnits.stream()
        .filter(Objects::nonNull)
        .map(Unit::getOwner)
        .filter(Objects::nonNull)
        .findAny()
        .orElse(PlayerId.NULL_PLAYERID);
  }

  /**
   * hits are removed from units. Note that units are removed in the order that the iterator will move through them.
   */
  private void selectCasualties(final DiceRoll dice, final Collection<Unit> allFriendlyUnits,
      final Collection<Unit> validTargetedUnitsForThisRoll, final Collection<Unit> defendingAa,
      final Collection<Unit> allEnemyUnits, final Territory territory, final String currentTypeAa) {
    final CasualtyDetails casualties = BattleCalculator.getAaCasualties(false, validTargetedUnitsForThisRoll,
        allFriendlyUnits, defendingAa, allEnemyUnits, dice, bridge, player, null, territory,
        TerritoryEffectHelper.getEffects(territory), false, new ArrayList<>());
    getRemotePlayer().reportMessage(casualties.size() + " " + currentTypeAa + " hits in " + territory.getName(),
        casualties.size() + " " + currentTypeAa + " hits in " + territory.getName());
    BattleDelegate.markDamaged(new ArrayList<>(casualties.getDamaged()), bridge);
    bridge.getHistoryWriter().addChildToEvent(
        MyFormatter.unitsToTextNoOwner(casualties.getKilled()) + " lost in " + territory.getName(),
        new ArrayList<>(casualties.getKilled()));
    allFriendlyUnits.removeAll(casualties.getKilled());
    if (this.casualties == null) {
      this.casualties = new ArrayList<>(casualties.getKilled());
    } else {
      this.casualties.addAll(casualties.getKilled());
    }
  }
}
