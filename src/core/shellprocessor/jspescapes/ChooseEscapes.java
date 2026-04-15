//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package core.shellprocessor.jspescapes;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import core.EasyI18N;
import core.ui.MainActivity;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

class ChooseEscapes extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    public JLabel escapesListLabel;
    public JComboBox escapesListComboBox;
    public JLabel isDoubleConfusionLabel;
    public JCheckBox isDoubleConfusionCheckBox;
    public JLabel isRandomConfusionLabel;
    public JCheckBox isRandomConfusionCheckBox;
    public JLabel EncodingLabel;
    public JComboBox EncodingComboBox;
    public JLabel EncodingTitleLabel;
    public JCheckBox EncodingCheckBox;
    public JLabel isAppendLitterLabel;
    public JCheckBox isAppendLitterCheckBox;
    public JLabel litterMinLengthLabel;
    public JTextField litterMinLengthTextField;
    public JLabel litterMaxLengthLabel;
    public JTextField litterMaxLengthTextField;
    public JLabel is;

    public ChooseEscapes() {
        this.$$$setupUI$$$();
        this.setContentPane(this.contentPane);
        this.setModal(true);
        this.getRootPane().setDefaultButton(this.buttonOK);
        this.buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ChooseEscapes.this.onOK();
            }
        });
        this.isRandomConfusionCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (ChooseEscapes.this.isRandomConfusionCheckBox.isSelected()) {
                    ChooseEscapes.this.escapesListComboBox.setEnabled(false);
                } else {
                    ChooseEscapes.this.escapesListComboBox.setEnabled(true);
                }

            }
        });
        EasyI18N.installObject(this);
    }

    public void onOK() {
        this.dispose();
    }

    public static JspEscapesProcessor.EscapesOptions chooseEscapes(String[] escapesMethods) {
        JspEscapesProcessor.EscapesOptions options = new JspEscapesProcessor.EscapesOptions();
        ChooseEscapes dialog = new ChooseEscapes();
        Arrays.stream(escapesMethods).forEach((method) -> {
            dialog.escapesListComboBox.addItem(method);
        });
        String[] IBMLIST = new String[]{"关闭", "cp037", "cp290", "utf-16le", "utf-16be", "utf-32le", "utf-32be", "IBM01145", "IBM01146"};
        Arrays.stream(IBMLIST).forEach((method) -> {
            dialog.EncodingComboBox.addItem(method);
        });
        dialog.setTitle(EasyI18N.getI18nString("混淆配置"));
        dialog.setLocationRelativeTo(MainActivity.getFrame());
        dialog.pack();
        dialog.setVisible(true);
        options.escapeMethod = dialog.escapesListComboBox.getSelectedItem().toString();
        options.EncodingMethod = dialog.EncodingComboBox.getSelectedItem().toString();
        options.isEncodingHeader = dialog.EncodingCheckBox.isSelected();
        options.isAppendLitter = dialog.isAppendLitterCheckBox.isSelected();
        options.isDoubleConfusion = dialog.isDoubleConfusionCheckBox.isSelected();
        options.isRandomConfusion = dialog.isRandomConfusionCheckBox.isSelected();
        options.minLitterNumber = Integer.parseInt(dialog.litterMinLengthTextField.getText());
        options.maxLitterNumber = Integer.parseInt(dialog.litterMaxLengthTextField.getText());
        return options;
    }

    private void $$$setupUI$$$() {
        this.contentPane = new JPanel();
        this.contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        this.contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, 0, 3, 3, 1, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.buttonOK = new JButton();
        this.buttonOK.setText("OK");
        panel2.add(this.buttonOK, new GridConstraints(0, 0, 1, 1, 0, 1, 3, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(8, 2, new Insets(0, 0, 0, 0), -1, -1));
        this.contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.escapesListLabel = new JLabel();
        this.escapesListLabel.setText("混淆列表:");
        panel3.add(this.escapesListLabel, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.escapesListComboBox = new JComboBox();
        panel3.add(this.escapesListComboBox, new GridConstraints(0, 1, 1, 1, 8, 1, 2, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.isDoubleConfusionLabel = new JLabel();
        this.isDoubleConfusionLabel.setText("是否双重混淆:");
        panel3.add(this.isDoubleConfusionLabel, new GridConstraints(4, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.isDoubleConfusionCheckBox = new JCheckBox();
        this.isDoubleConfusionCheckBox.setText("开启");
        panel3.add(this.isDoubleConfusionCheckBox, new GridConstraints(4, 1, 1, 1, 8, 0, 3, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.isRandomConfusionLabel = new JLabel();
        this.isRandomConfusionLabel.setText("是否开启随机混淆:");
        panel3.add(this.isRandomConfusionLabel, new GridConstraints(5, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.isRandomConfusionCheckBox = new JCheckBox();
        this.isRandomConfusionCheckBox.setText("开启");
        panel3.add(this.isRandomConfusionCheckBox, new GridConstraints(5, 1, 1, 1, 8, 0, 3, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.EncodingLabel = new JLabel();
        this.EncodingLabel.setText("编码（编码后请勿复制粘贴传输）:");
        panel3.add(this.EncodingLabel, new GridConstraints(6, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.EncodingComboBox = new JComboBox();
        panel3.add(this.EncodingComboBox, new GridConstraints(6, 1, 1, 1, 8, 1, 2, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.EncodingTitleLabel = new JLabel();
        this.EncodingTitleLabel.setText("编码头部声明（不勾选此选项可能导致不解析）:");
        panel3.add(this.EncodingTitleLabel, new GridConstraints(7, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.EncodingCheckBox = new JCheckBox();
        this.EncodingCheckBox.setText("开启");
        this.EncodingCheckBox.setSelected(true);
        panel3.add(this.EncodingCheckBox, new GridConstraints(7, 1, 1, 1, 8, 0, 3, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.isAppendLitterLabel = new JLabel();
        this.isAppendLitterLabel.setText("是否追加垃圾数据:");
        panel3.add(this.isAppendLitterLabel, new GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.isAppendLitterCheckBox = new JCheckBox();
        this.isAppendLitterCheckBox.setText("开启");
        panel3.add(this.isAppendLitterCheckBox, new GridConstraints(1, 1, 1, 1, 8, 0, 3, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.litterMinLengthLabel = new JLabel();
        this.litterMinLengthLabel.setText("垃圾数据最小长度:");
        panel3.add(this.litterMinLengthLabel, new GridConstraints(2, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.litterMinLengthTextField = new JTextField();
        this.litterMinLengthTextField.setText("2");
        panel3.add(this.litterMinLengthTextField, new GridConstraints(2, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.litterMaxLengthLabel = new JLabel();
        this.litterMaxLengthLabel.setText("垃圾数据最大长度:");
        panel3.add(this.litterMaxLengthLabel, new GridConstraints(3, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.litterMaxLengthTextField = new JTextField();
        this.litterMaxLengthTextField.setText("5");
        panel3.add(this.litterMaxLengthTextField, new GridConstraints(3, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
    }

    public JComponent $$$getRootComponent$$$() {
        return this.contentPane;
    }

    private void createUIComponents() {
    }
}
