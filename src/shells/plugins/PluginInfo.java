//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.plugins;

public class PluginInfo {
    private String PluginName;
    private String DisplayName;
    private int LoadType;
    private boolean LoadState;

    public PluginInfo(String Name, String DisplayName, int LoadType) {
        this.PluginName = Name;
        this.DisplayName = DisplayName;
        this.LoadType = LoadType;
        this.LoadState = false;
    }

    public String getPluginName() {
        return this.PluginName;
    }

    public String getDisplayName() {
        return this.DisplayName;
    }

    public int getLoadType() {
        return this.LoadType;
    }

    public void setPluginName(String Name) {
        this.PluginName = Name;
    }

    public boolean setLoadState(boolean LoadState) {
        return this.LoadState = LoadState;
    }

    public void setDisplayName(String DisplayName) {
        this.DisplayName = DisplayName;
    }

    public void setLoadType(int LoadType) {
        this.LoadType = LoadType;
    }

    public boolean getLoadState() {
        return this.LoadState;
    }
}
