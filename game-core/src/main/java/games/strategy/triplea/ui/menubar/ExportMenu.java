package games.strategy.triplea.ui.menubar;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.annotation.Nullable;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.triplea.swing.SwingAction;
import org.triplea.util.FileNameUtils;

import com.google.common.collect.Iterables;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.export.GameDataExporter;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.printgenerator.SetupFrame;
import games.strategy.triplea.ui.ExtendedStats;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.export.ScreenshotExporter;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.triplea.util.PlayerOrderComparator;
import lombok.extern.java.Log;

@Log
final class ExportMenu extends JMenu {
  private static final long serialVersionUID = 8416990293444575737L;

  private final TripleAFrame frame;
  private final GameData gameData;
  private final UiContext uiContext;
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");

  ExportMenu(final TripleAFrame frame) {
    super("Export");

    this.frame = frame;
    gameData = frame.getGame().getData();
    uiContext = frame.getUiContext();

    setMnemonic(KeyEvent.VK_E);

    addExportXml();
    addExportStats();
    addExportStatsFull();
    addExportSetupCharts();
    addExportUnitStats();
    addSaveScreenshot();
  }

  // TODO: create a second menu option for parsing current attachments
  private void addExportXml() {
    final Action exportXml = SwingAction.of("Export game.xml File (Beta)", e -> exportXmlFile());
    add(exportXml).setMnemonic(KeyEvent.VK_X);
  }

  private void exportXmlFile() {
    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    final File rootDir = new File(SystemProperties.getUserDir());

    final int round = gameData.getCurrentRound();
    final String defaultFileName = FileNameUtils.removeIllegalCharacters(
        String.format("xml_%s_%s_round_%s",
            dateTimeFormatter.format(LocalDateTime.now(ZoneId.systemDefault())), gameData.getGameName(), round))
        + ".xml";
    chooser.setSelectedFile(new File(rootDir, defaultFileName));
    if (chooser.showSaveDialog(frame) != JOptionPane.OK_OPTION) {
      return;
    }
    final String xmlFile;
    try {
      gameData.acquireReadLock();
      final GameDataExporter exporter = new GameDataExporter(gameData);
      xmlFile = exporter.getXml();
    } finally {
      gameData.releaseReadLock();
    }
    try (Writer writer = Files.newBufferedWriter(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
      writer.write(xmlFile);
    } catch (final IOException e1) {
      log.log(Level.SEVERE, "Failed to write XML: " + chooser.getSelectedFile().getAbsolutePath(), e1);
    }
  }

  private void addSaveScreenshot() {
    final Action abstractAction = SwingAction.of("Export Map Snapshot", e -> {
      // get current history node. if we are in history view, get the selected node.
      final HistoryPanel historyPanel = frame.getHistoryPanel();
      final HistoryNode curNode;
      if (historyPanel == null) {
        curNode = gameData.getHistory().getLastNode();
      } else {
        curNode = historyPanel.getCurrentNode();
      }
      ScreenshotExporter.exportScreenshot(frame, gameData, curNode);
    });
    add(abstractAction).setMnemonic(KeyEvent.VK_E);
  }

  private void addExportStatsFull() {
    final Action showDiceStats = SwingAction.of("Export Full Game Stats", e -> createAndSaveStats(true));
    add(showDiceStats).setMnemonic(KeyEvent.VK_F);
  }

  private void addExportStats() {
    final Action showDiceStats = SwingAction.of("Export Short Game Stats", e -> createAndSaveStats(false));
    add(showDiceStats).setMnemonic(KeyEvent.VK_S);
  }

  private void createAndSaveStats(final boolean showPhaseStats) {
    final ExtendedStats statPanel = new ExtendedStats(gameData, uiContext);
    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    final File rootDir = new File(SystemProperties.getUserDir());
    final int currentRound = gameData.getCurrentRound();
    final String defaultFileName = FileNameUtils.removeIllegalCharacters(
        String.format("stats_%s_%s_round_%s_%s",
            dateTimeFormatter.format(LocalDateTime.now(ZoneId.systemDefault())), gameData.getGameName(),
            currentRound, showPhaseStats ? "full" : "short"))
        + ".csv";
    chooser.setSelectedFile(new File(rootDir, defaultFileName));
    if (chooser.showSaveDialog(frame) != JOptionPane.OK_OPTION) {
      return;
    }
    try (PrintWriter writer = new PrintWriter(chooser.getSelectedFile(), StandardCharsets.UTF_8.toString())) {
      gameData.acquireReadLock();
      final GameData clone = GameDataUtils.cloneGameData(gameData);
      writer.append(defaultFileName).println(',');
      writer.append("TripleA Engine Version: ,");
      writer.append(ClientContext.engineVersion().toString()).println(',');
      writer.append("Game Name: ,");
      writer.append(gameData.getGameName()).println(',');
      writer.append("Game Version: ,");
      writer.append(gameData.getGameVersion().toString()).println(',');
      writer.println();
      writer.append("Current Round: ,");
      writer.print(currentRound);
      writer.println(',');
      writer.append("Number of Players: ,");
      writer.print(statPanel.getPlayers().size());
      writer.println(',');
      writer.append("Number of Alliances: ,");
      writer.print(statPanel.getAlliances().size());
      writer.println(',');
      writer.println();
      writer.println("Turn Order: ,");
      final SortedSet<PlayerId> orderedPlayers = new TreeSet<>(new PlayerOrderComparator(gameData));
      orderedPlayers.addAll(gameData.getPlayerList().getPlayers());
      for (final PlayerId currentPlayerId : orderedPlayers) {
        writer.append(currentPlayerId.getName()).append(',');
        final Collection<String> allianceNames = gameData.getAllianceTracker().getAlliancesPlayerIsIn(currentPlayerId);
        for (final String allianceName : allianceNames) {
          writer.append(allianceName).append(',');
        }
        writer.println();
      }
      writer.println();
      writer.append("Winners: ,");
      final EndRoundDelegate delegateEndRound = (EndRoundDelegate) gameData.getDelegate("endRound");
      if (delegateEndRound != null && delegateEndRound.getWinners() != null) {
        for (final PlayerId p : delegateEndRound.getWinners()) {
          writer.append(p.getName()).append(',');
        }
      } else {
        writer.append("none yet; game not over,");
      }
      writer.println();
      writer.println();
      writer.println("Resource Chart: ,");
      for (final Resource resource : gameData.getResourceList().getResources()) {
        writer.append(resource.getName()).println(',');
      }
      // if short, we won't both showing production and unit info
      if (showPhaseStats) {
        writer.println();
        writer.println("Production Rules: ,");
        writer.append("Name,Result,Quantity,Cost,Resource,\n");
        final Collection<ProductionRule> purchaseOptions = gameData.getProductionRuleList().getProductionRules();
        for (final ProductionRule pr : purchaseOptions) {
          final String costString = pr.toStringCosts().replaceAll(";? ", ",");
          writer.append(pr.getName()).append(',');
          writer.append(pr.getResults().keySet().iterator().next().getName()).append(',');
          writer.print(pr.getResults().getInt(pr.getResults().keySet().iterator().next()));
          writer.append(',').append(costString).println(',');
        }
        writer.println();
        writer.println("Unit Types: ,");
        writer.append("Name,Listed Abilities\n");
        for (final UnitType unitType : gameData.getUnitTypeList()) {
          final UnitAttachment ua = UnitAttachment.get(unitType);
          if (ua == null) {
            continue;
          }
          final String toModify = ua.allUnitStatsForExporter()
              .replaceAll("UnitType called | with:|games\\.strategy\\.engine\\.data\\.", "")
              .replaceAll("[\n,]", ";")
              .replaceAll(" {2}| ?, ?", ",");
          writer.println(toModify);
        }
      }
      writer.println();
      writer.println(showPhaseStats ? "Full Stats (includes each phase that had activity),"
          : "Short Stats (only shows first phase with activity per player per round),");
      writer.println("Turn Stats: ,");
      writer.append("Round,Player Turn,Phase Name,");
      final String[] alliances = statPanel.getAlliances().toArray(new String[0]);
      final PlayerId[] players = statPanel.getPlayers().toArray(new PlayerId[0]);
      // its important here to translate the player objects into our game data
      // the players for the stat panel are only relevant with respect to the game data they belong to
      Arrays.setAll(players, i -> clone.getPlayerList().getPlayerId(players[i].getName()));

      // extended stats covers stuff that doesn't show up in the game stats menu bar, like custom resources or tech
      // tokens or # techs, etc.
      final Iterable<IStat> stats = Iterables.concat(
          Arrays.asList(statPanel.getStats()),
          Arrays.asList(statPanel.getStatsExtended(gameData)));
      for (final IStat stat : stats) {
        for (final PlayerId player : players) {
          writer.append(stat.getName()).append(' ');
          writer.append(player.getName()).append(',');
        }
        for (final String alliance : alliances) {
          writer.append(stat.getName()).append(' ');
          writer.append(alliance).append(',');
        }
      }
      writer.println();
      clone.getHistory().gotoNode(clone.getHistory().getLastNode());
      @SuppressWarnings("unchecked")
      final Enumeration<TreeNode> nodes = ((DefaultMutableTreeNode) clone.getHistory().getRoot()).preorderEnumeration();
      @Nullable
      PlayerId currentPlayer = null;
      int round = 0;
      while (nodes.hasMoreElements()) {
        // we want to export on change of turn
        final HistoryNode element = (HistoryNode) nodes.nextElement();
        if (element instanceof Round) {
          round++;
        }
        if (!(element instanceof Step)) {
          continue;
        }
        final Step step = (Step) element;
        if (step.getPlayerId() == null || step.getPlayerId().isNull()) {
          continue;
        }
        // this is to stop from having multiple entries for each players turn.
        if (!showPhaseStats) {
          if (Objects.equals(step.getPlayerId(), currentPlayer)) {
            continue;
          }
        }
        currentPlayer = step.getPlayerId();
        clone.getHistory().gotoNode(element);
        final String playerName = step.getPlayerId() == null ? "" : step.getPlayerId().getName() + ": ";
        String stepName = step.getStepName();
        // copied directly from TripleAPlayer, will probably have to be updated in the future if more delegates are made
        if (stepName.endsWith("Bid")) {
          stepName = "Bid";
        } else if (stepName.endsWith("Tech")) {
          stepName = "Tech";
        } else if (stepName.endsWith("TechActivation")) {
          stepName = "TechActivation";
        } else if (stepName.endsWith("Purchase")) {
          stepName = "Purchase";
        } else if (stepName.endsWith("NonCombatMove")) {
          stepName = "NonCombatMove";
        } else if (stepName.endsWith("Move")) {
          stepName = "Move";
        } else if (stepName.endsWith("Battle")) {
          stepName = "Battle";
        } else if (stepName.endsWith("BidPlace")) {
          stepName = "BidPlace";
        } else if (stepName.endsWith("Place")) {
          stepName = "Place";
        } else if (stepName.endsWith("Politics")) {
          stepName = "Politics";
        } else if (stepName.endsWith("EndTurn")) {
          stepName = "EndTurn";
        } else {
          stepName = "";
        }
        writer.print(round);
        writer.append(',').append(playerName).append(',').append(stepName).append(',');
        for (final IStat stat : stats) {
          for (final PlayerId player : players) {
            writer.append(stat.getFormatter().format(stat.getValue(player, clone))).append(',');
          }
          for (final String alliance : alliances) {
            writer.append(stat.getFormatter().format(stat.getValue(alliance, clone))).append(',');
          }
        }
        writer.println();
      }
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to write stats: " + chooser.getSelectedFile().getAbsolutePath(), e);
    } finally {
      gameData.releaseReadLock();
    }
  }

  private void addExportUnitStats() {
    final JMenuItem menuFileExport = new JMenuItem(SwingAction.of("Export Unit Charts", e -> {
      final JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      final File rootDir = new File(SystemProperties.getUserDir());
      String defaultFileName = gameData.getGameName() + "_unit_stats";
      defaultFileName = FileNameUtils.removeIllegalCharacters(defaultFileName);
      defaultFileName = defaultFileName + ".html";
      chooser.setSelectedFile(new File(rootDir, defaultFileName));
      if (chooser.showSaveDialog(frame) != JOptionPane.OK_OPTION) {
        return;
      }
      try (Writer writer = Files.newBufferedWriter(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
        writer.write(HelpMenu
            .getUnitStatsTable(gameData, uiContext)
            .replaceAll("</?p>|</tr>", "$0\r\n")
            .replaceAll("(?i)<img[^>]+/>", ""));
      } catch (final IOException e1) {
        log.log(Level.SEVERE, "Failed to write unit stats: " + chooser.getSelectedFile().getAbsolutePath(), e1);
      }
    }));
    menuFileExport.setMnemonic(KeyEvent.VK_U);
    add(menuFileExport);
  }

  private void addExportSetupCharts() {
    final JMenuItem menuFileExport = new JMenuItem(SwingAction.of("Export Setup Charts", e -> {
      final JFrame frame = new JFrame("Export Setup Charts");
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      final GameData clonedGameData;
      gameData.acquireReadLock();
      try {
        clonedGameData = GameDataUtils.cloneGameData(gameData);
      } finally {
        gameData.releaseReadLock();
      }
      final JComponent newContentPane = new SetupFrame(clonedGameData);
      // content panes must be opaque
      newContentPane.setOpaque(true);
      frame.setContentPane(newContentPane);
      // Display the window.
      frame.pack();
      frame.setLocationRelativeTo(frame);
      frame.setVisible(true);
      uiContext.addShutdownWindow(frame);
    }));
    menuFileExport.setMnemonic(KeyEvent.VK_C);
    add(menuFileExport);
  }
}
