package games.strategy.triplea.ui.menubar;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.triplea.swing.SwingComponents;

import games.strategy.triplea.UrlConstants;

final class WebHelpMenu extends JMenu {
  private static final long serialVersionUID = -1940188637908722947L;

  WebHelpMenu() {
    super("Web");

    setMnemonic(KeyEvent.VK_W);

    addWebMenu();
  }

  private void addWebMenu() {
    final JMenuItem hostingLink = new JMenuItem("User Guide");
    hostingLink.setMnemonic(KeyEvent.VK_H);
    hostingLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.USER_GUIDE));
    add(hostingLink);

    final JMenuItem warClub = new JMenuItem("TripleA Forum");
    warClub.setMnemonic(KeyEvent.VK_W);
    warClub.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_FORUM));
    add(warClub);

    final JMenuItem ruleBookLink = new JMenuItem("Rule Book");
    ruleBookLink.setMnemonic(KeyEvent.VK_K);
    ruleBookLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.RULE_BOOK));
    add(ruleBookLink);

    final JMenuItem donateLink = new JMenuItem("Donate");
    donateLink.setMnemonic(KeyEvent.VK_O);
    donateLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.PAYPAL_DONATE));
    add(donateLink);
  }
}
