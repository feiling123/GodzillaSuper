//
// RASP \u57fa\u51c6 B1\uff08JNI \u8de8\u8fb9\u754c\uff09\u63a7\u5236\u7aef\u63d2\u4ef6\u3002
// \u4ec5\u7528\u4e8e\u6388\u6743\u9694\u79bb\u73af\u5883\uff1bJNI \u5728\u672c\u8fdb\u7a0b\uff08Godzilla \u5ba2\u6237\u7aef JVM\uff09\u5185\u6267\u884c\u3002
//

package shells.plugins.java;

import core.Encoding;
import core.annotation.PluginAnnotation;
import core.imp.Payload;
import core.imp.Plugin;
import core.shell.ShellEntity;
import core.ui.component.RTextArea;
import core.ui.component.dialog.GOptionPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import util.Log;
import util.automaticBindClick;

@PluginAnnotation(
        payloadName = "JavaDynamicPayload",
        Name = "JNINativeBench",
        DisplayName = "JNI\u57fa\u51c6B1"
)
public class JNINativeBenchPlugin implements Plugin {

    private ShellEntity shellEntity;
    private Payload payload;
    private Encoding encoding;

    private final JPanel panel = new JPanel(new BorderLayout(0, 10));
    private final JTextField nativeLibPathField = new JTextField();
    private final JTextField commandField = new JTextField();
    private final RTextArea resultTextArea = new RTextArea();
    private final JButton selectLibButton = new JButton("\u6d4f\u89c8\u2026");
    private final JButton runBenchButton = new JButton("\u672c\u5730 JNI \u6267\u884c");

    private static volatile boolean nativeLoaded;
    private static String loadedPath;

    private static native String raspBenchNativeExec(String cmd);

    public JNINativeBenchPlugin() {
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        form.add(new JLabel("\u52a8\u6001\u5e93\uff1a"), c);
        c.gridx = 1;
        c.weightx = 1;
        nativeLibPathField.setToolTipText(".dll / .so / .dylib\uff0c\u4e0e\u5f53\u524d JDK \u67b6\u6784\u4e00\u81f4");
        form.add(nativeLibPathField, c);
        c.gridx = 2;
        c.weightx = 0;
        selectLibButton.setPreferredSize(new Dimension(88, 28));
        form.add(selectLibButton, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        form.add(new JLabel("\u547d\u4ee4\uff1a"), c);
        c.gridx = 1;
        c.weightx = 1;
        commandField.setText("echo RASP_BENCH_B1_OK");
        commandField.setToolTipText("\u4f20\u9012\u7ed9\u539f\u751f raspBenchNativeExec");
        form.add(commandField, c);
        c.gridx = 2;
        c.weightx = 0;
        runBenchButton.setPreferredSize(new Dimension(120, 28));
        form.add(runBenchButton, c);

        panel.add(form, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(resultTextArea);
        scroll.setPreferredSize(new Dimension(560, 240));
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        "\u56de\u663e / \u8f93\u51fa"),
                new EmptyBorder(4, 6, 6, 6)));
        panel.add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "<html><body style='width:520px'><small style='color:#555'>\u672c\u63d2\u4ef6\u5728<strong>\u63a7\u5236\u7aef JVM</strong>\u52a0\u8f7d JNI\uff0c\u7528\u4e8e\u5bf9\u540c\u4e00\u8fdb\u7a0b\u5185\u6302\u8f7d\u7684 RASP Agent \u505a B1 \u57fa\u7ebf\u6d4b\u8bd5\u3002"
                        + "\u8fdc\u7aef\u5e94\u7528\u8bc4\u6d4b\u8bf7\u5355\u72ec\u4e0b\u53d1 helper \u7c7b\u3002</small></body></html>");
        hint.setBorder(new EmptyBorder(4, 0, 0, 0));
        panel.add(hint, BorderLayout.SOUTH);
    }

    private void selectLibButtonClick(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Native library (.so/.dll/.dylib)", "so", "dll", "dylib"));
        if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            nativeLibPathField.setText(f.getAbsolutePath());
        }
    }

    private void runBenchButtonClick(ActionEvent e) {
        String path = nativeLibPathField.getText().trim();
        if (path.isEmpty()) {
            GOptionPane.showMessageDialog(panel, "\u8bf7\u9009\u62e9 .so / .dll / .dylib", "\u63d0\u793a", 2);
            return;
        }
        File lib = new File(path);
        if (!lib.isFile()) {
            GOptionPane.showMessageDialog(panel, "\u6587\u4ef6\u4e0d\u5b58\u5728", "\u9519\u8bef", 0);
            return;
        }
        try {
            synchronized (JNINativeBenchPlugin.class) {
                if (!nativeLoaded || !path.equals(loadedPath)) {
                    System.load(lib.getAbsolutePath());
                    nativeLoaded = true;
                    loadedPath = path;
                }
            }
            String cmd = commandField.getText();
            if (cmd == null || cmd.isEmpty()) {
                cmd = "echo RASP_BENCH_B1_OK";
            }
            String out = raspBenchNativeExec(cmd);
            resultTextArea.append("---- " + new java.util.Date() + " ----\n");
            resultTextArea.append(out != null ? out : "(null)");
            resultTextArea.append("\n\n");
            resultTextArea.setCaretPosition(resultTextArea.getDocument().getLength());
            Log.log("[JNINativeBench] done, len=%s", new Object[]{out != null ? out.length() : 0});
        } catch (UnsatisfiedLinkError ule) {
            Log.error(ule);
            resultTextArea.append("\u52a0\u8f7d JNI \u5931\u8d25\uff1a\n" + ule + "\n\n"
                    + "\u8bf7\u7528\u4e0e\u5f53\u524d OS/JDK \u67b6\u6784\u5339\u914d\u7684\u5e93\uff0c\u5e76\u786e\u8ba4\u51fd\u6570\u540d\uff1a\n"
                    + "Java_shells_plugins_java_JNINativeBenchPlugin_raspBenchNativeExec\n\n");
            GOptionPane.showMessageDialog(panel, ule.getMessage(), "JNI", 0);
        } catch (Throwable t) {
            Log.error(t);
            resultTextArea.append(String.valueOf(t) + "\n\n");
            GOptionPane.showMessageDialog(panel, t.getMessage(), "\u9519\u8bef", 0);
        }
    }

    @Override
    public void init(ShellEntity shellEntity) {
        this.shellEntity = shellEntity;
        this.payload = shellEntity.getPayloadModule();
        this.encoding = Encoding.getEncoding(shellEntity);
        automaticBindClick.bindJButtonClick(this, this);
    }

    @Override
    public JPanel getView() {
        return panel;
    }
}
