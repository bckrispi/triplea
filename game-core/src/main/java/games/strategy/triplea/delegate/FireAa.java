package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.triplea.java.Interruptibles;
import org.triplea.java.collections.CollectionUtils;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.sound.SoundUtils;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.MustFightBattle.ReturnFire;
import games.strategy.triplea.delegate.data.CasualtyDetails;

/**
 * Maintains the state of a group of AA units firing during a {@link MustFightBattle}.
 */
public class FireAa implements IExecutable {
  private static final long serialVersionUID = -6406659798754841382L;

  private final Collection<Unit> firingUnits;
  private final Collection<Unit> attackableUnits;
  private final MustFightBattle battle;
  private final PlayerId firingPlayer;
  private final PlayerId hitPlayer;
  private final boolean defending;
  private final Map<Unit, Collection<Unit>> dependentUnits;
  private final GUID battleId;
  private final boolean headless;
  private final Territory battleSite;
  private final Collection<TerritoryEffect> territoryEffects;
  private final List<Unit> allFriendlyUnitsAliveOrWaitingToDie;
  private final List<Unit> allEnemyUnitsAliveOrWaitingToDie;
  private final boolean isAmphibious;
  private final Collection<Unit> amphibiousLandAttackers;
  private final List<String> aaTypes;

  // These variables change state during execution
  private DiceRoll dice;
  private CasualtyDetails casualties;
  private final Collection<Unit> casualtiesSoFar = new ArrayList<>();

  FireAa(final Collection<Unit> attackableUnits, final PlayerId firingPlayer, final PlayerId hitPlayer,
      final Collection<Unit> firingUnits, final MustFightBattle battle, final boolean defending,
      final Map<Unit, Collection<Unit>> dependentUnits, final boolean headless, final Territory battleSite,
      final Collection<TerritoryEffect> territoryEffects, final List<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final List<Unit> allEnemyUnitsAliveOrWaitingToDie, final List<String> aaTypes) {
    this.attackableUnits = CollectionUtils.getMatches(attackableUnits, Matches.unitIsNotInfrastructure());
    this.firingUnits = firingUnits;
    this.battle = battle;
    this.hitPlayer = hitPlayer;
    this.firingPlayer = firingPlayer;
    this.defending = defending;
    this.dependentUnits = dependentUnits;
    this.headless = headless;
    battleId = battle.getBattleId();
    this.battleSite = battleSite;
    this.territoryEffects = territoryEffects;
    this.allFriendlyUnitsAliveOrWaitingToDie = allFriendlyUnitsAliveOrWaitingToDie;
    this.allEnemyUnitsAliveOrWaitingToDie = allEnemyUnitsAliveOrWaitingToDie;
    isAmphibious = this.battle.isAmphibious();
    amphibiousLandAttackers = this.battle.getAmphibiousLandAttackers();
    this.aaTypes = aaTypes;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {

    // Loop through each type of AA and break into firing groups based on suicideOnHit
    for (final String aaType : aaTypes) {
      final Collection<Unit> aaTypeUnits = CollectionUtils.getMatches(firingUnits, Matches.unitIsAaOfTypeAa(aaType));
      final List<Collection<Unit>> firingGroups = MustFightBattle.newFiringUnitGroups(aaTypeUnits);
      for (final Collection<Unit> firingGroup : firingGroups) {
        final Set<UnitType> validTargetTypes =
            UnitAttachment.get(firingGroup.iterator().next().getType()).getTargetsAa(bridge.getData());
        final Set<UnitType> airborneTypesTargeted =
            defending ? TechAbilityAttachment.getAirborneTargettedByAa(hitPlayer, bridge.getData()).get(aaType)
                : new HashSet<>();
        final Collection<Unit> validTargets = CollectionUtils
            .getMatches(attackableUnits, Matches.unitIsOfTypes(validTargetTypes)
                .or(Matches.unitIsAirborne().and(Matches.unitIsOfTypes(airborneTypesTargeted))));
        final IExecutable rollDice = new IExecutable() {
          private static final long serialVersionUID = 6435935558879109347L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            validTargets.removeAll(casualtiesSoFar);
            if (!validTargets.isEmpty()) {
              dice = DiceRoll.rollAa(validTargets, firingGroup, allEnemyUnitsAliveOrWaitingToDie,
                  allFriendlyUnitsAliveOrWaitingToDie, bridge, battleSite, defending);
              if (!headless) {
                SoundUtils.playFireBattleAa(firingPlayer, aaType, dice.getHits() > 0, bridge);
              }
            }
          }
        };
        final IExecutable selectCasualties = new IExecutable() {
          private static final long serialVersionUID = 7943295620796835166L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!validTargets.isEmpty()) {
              final CasualtyDetails details = selectCasualties(validTargets, firingGroup, bridge, aaType);
              battle.markDamaged(details.getDamaged(), bridge);
              casualties = details;
              casualtiesSoFar.addAll(details.getKilled());
            }
          }
        };
        final IExecutable notifyCasualties = new IExecutable() {
          private static final long serialVersionUID = -6759782085212899725L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!validTargets.isEmpty()) {
              notifyCasualtiesAa(bridge, aaType);
              battle.removeCasualties(casualties.getKilled(), ReturnFire.ALL, !defending, bridge);
              battle.removeSuicideOnHitCasualties(firingGroup, dice.getHits(), defending, bridge);
            }
          }
        };
        // push in reverse order of execution
        stack.push(notifyCasualties);
        stack.push(selectCasualties);
        stack.push(rollDice);
      }
    }
  }

  private CasualtyDetails selectCasualties(final Collection<Unit> validAttackingUnitsForThisRoll,
      final Collection<Unit> defendingAa, final IDelegateBridge bridge, final String currentTypeAa) {
    // send defender the dice roll so he can see what the dice are while he waits for attacker to select casualties
    AbstractBattle.getDisplay(bridge).notifyDice(dice,
        hitPlayer.getName() + BattleStepStrings.SELECT_PREFIX + currentTypeAa + BattleStepStrings.CASUALTIES_SUFFIX);
    return BattleCalculator.getAaCasualties(!defending, validAttackingUnitsForThisRoll, attackableUnits, defendingAa,
        firingUnits, dice, bridge, hitPlayer, battleId, battleSite, territoryEffects, isAmphibious,
        amphibiousLandAttackers);
  }

  private void notifyCasualtiesAa(final IDelegateBridge bridge, final String currentTypeAa) {
    if (headless) {
      return;
    }
    AbstractBattle.getDisplay(bridge).casualtyNotification(battleId,
        hitPlayer.getName() + BattleStepStrings.REMOVE_PREFIX + currentTypeAa + BattleStepStrings.CASUALTIES_SUFFIX,
        dice, hitPlayer, new ArrayList<>(casualties.getKilled()),
        new ArrayList<>(casualties.getDamaged()), dependentUnits);
    AbstractBattle.getRemote(hitPlayer, bridge).confirmOwnCasualties(battleId, "Press space to continue");
    final Thread t = new Thread(() -> {
      try {
        AbstractBattle.getRemote(firingPlayer, bridge).confirmEnemyCasualties(battleId, "Press space to continue",
            hitPlayer);
      } catch (final Exception e) {
        // ignore
      }
    }, "click to continue waiter");
    t.start();
    bridge.leaveDelegateExecution();
    Interruptibles.join(t);
    bridge.enterDelegateExecution();
  }

}
