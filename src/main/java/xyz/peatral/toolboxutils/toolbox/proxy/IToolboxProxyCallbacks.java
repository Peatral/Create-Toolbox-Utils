package xyz.peatral.toolboxutils.toolbox.proxy;

public interface IToolboxProxyCallbacks {
    void onSetChanged();
    void onSendData();
    void onLazyTick();
    void onInitialize();
    void onInvalidate();
}
