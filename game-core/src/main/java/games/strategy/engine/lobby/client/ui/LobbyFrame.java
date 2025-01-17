package games.strategy.engine.lobby.client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;

import org.triplea.lobby.common.IModeratorController;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.SwingAction;

import com.google.common.collect.ImmutableList;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatPlayerPanel;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.moderator.toolbox.ShowToolboxController;
import games.strategy.net.INode;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.menubar.LobbyMenu;

/**
 * The top-level frame window for the lobby client UI.
 */
public class LobbyFrame extends JFrame {
  private static final long serialVersionUID = -388371674076362572L;

  private final LobbyClient client;
  private final ChatMessagePanel chatMessagePanel;

  public LobbyFrame(final LobbyClient client, final LobbyServerProperties lobbyServerProperties) {
    super("TripleA Lobby");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setIconImage(JFrameBuilder.getGameIcon());
    this.client = client;
    setJMenuBar(new LobbyMenu(this));
    final Chat chat = new Chat(
        client.getMessengers(), LobbyConstants.LOBBY_CHAT, Chat.ChatSoundProfile.LOBBY_CHATROOM);
    chatMessagePanel = new ChatMessagePanel(chat);
    lobbyServerProperties.getServerMessage().ifPresent(chatMessagePanel::addServerMessage);
    chatMessagePanel.setShowTime(true);
    final ChatPlayerPanel chatPlayers = new ChatPlayerPanel(null);
    chatPlayers.addHiddenPlayerName(LobbyConstants.ADMIN_USERNAME);
    chatPlayers.setChat(chat);
    chatPlayers.setPreferredSize(new Dimension(200, 600));
    chatPlayers.addActionFactory(this::newAdminActions);
    final LobbyGamePanel gamePanel = new LobbyGamePanel(this.client.getMessengers());
    final JSplitPane leftSplit = new JSplitPane();
    leftSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    leftSplit.setTopComponent(gamePanel);
    leftSplit.setBottomComponent(chatMessagePanel);
    leftSplit.setResizeWeight(0.5);
    gamePanel.setPreferredSize(new Dimension(700, 200));
    chatMessagePanel.setPreferredSize(new Dimension(700, 400));
    final JSplitPane mainSplit = new JSplitPane();
    mainSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    mainSplit.setLeftComponent(leftSplit);
    mainSplit.setRightComponent(chatPlayers);
    mainSplit.setResizeWeight(1);
    add(mainSplit, BorderLayout.CENTER);
    pack();
    chatMessagePanel.requestFocusInWindow();
    setLocationRelativeTo(null);
    this.client.getMessengers().addErrorListener((reason) -> connectionToServerLost());
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        shutdown();
      }
    });
  }

  public ChatMessagePanel getChatMessagePanel() {
    return chatMessagePanel;
  }

  private List<Action> newAdminActions(final INode clickedOn) {
    if (!client.isAdmin()) {
      return Collections.emptyList();
    }
    if (clickedOn.equals(client.getMessengers().getLocalNode())) {
      return Collections.emptyList();
    }
    final IModeratorController controller = (IModeratorController) client.getMessengers()
        .getRemote(IModeratorController.REMOTE_NAME);
    final List<Action> actions = new ArrayList<>();
    actions.add(SwingAction.of("Boot " + clickedOn.getName(), e -> {
      if (!confirm("Boot " + clickedOn.getName())) {
        return;
      }
      controller.boot(clickedOn);
    }));
    actions.add(SwingAction.of("Ban Player", e -> {
      TimespanDialog.prompt(this, "Select Timespan",
          "Please consult other admins before banning longer than 1 day. \n"
              + "And please remember to report this ban.",
          date -> {
            controller.banMac(clickedOn, date);
            controller.boot(clickedOn);
          });
    }));

    actions.add(SwingAction.of("Show player information", e -> {
      final String text = controller.getInformationOn(clickedOn);
      final JTextPane textPane = new JTextPane();
      textPane.setEditable(false);
      textPane.setText(text);
      JOptionPane.showMessageDialog(null, textPane, "Player Info", JOptionPane.INFORMATION_MESSAGE);
    }));

    if (ClientSetting.showBetaFeatures.getValue().orElse(false)) {
      actions.add(
          SwingAction.of("(Beta) Moderator Toolbox", e -> ShowToolboxController.showToolbox(this)));
    }
    return ImmutableList.copyOf(actions);
  }

  private boolean confirm(final String question) {
    final int selectionOption = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(this), question,
        "Question", JOptionPane.OK_CANCEL_OPTION);
    return selectionOption == JOptionPane.OK_OPTION;
  }

  public LobbyClient getLobbyClient() {
    return client;
  }

  public void setShowChatTime(final boolean showTime) {
    if (chatMessagePanel != null) {
      chatMessagePanel.setShowTime(showTime);
    }
  }

  public void shutdown() {
    setVisible(false);
    dispose();
    new Thread(() -> {
      GameRunner.showMainFrame();
      client.getMessengers().shutDown();
      GameRunner.exitGameIfFinished();
    }).start();
  }

  private void connectionToServerLost() {
    EventThreadJOptionPane.showMessageDialog(LobbyFrame.this,
        "Connection to Server Lost.  Please close this instance and reconnect to the lobby.", "Connection Lost",
        JOptionPane.ERROR_MESSAGE);
  }
}
