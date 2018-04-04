package environment;

import models.StoreSettings;

/**
 * Created by K750JB on 24.03.2018.
 */
public class Store {
    private static Store ourInstance = new Store();
    public static Store getInstance() {
        return ourInstance;
    }
    private Store() {
    }

    private int InitialProductsCount;
    private String Name;

    private int CurrentProductsCount;

    public Store Initialize(StoreSettings storeSettings) {
        InitialProductsCount = storeSettings.ProductsCount;
        CurrentProductsCount = storeSettings.ProductsCount;
        Name = storeSettings.Name;

        return this;
    }

    public int GetCurrentProductsCount() {
        return CurrentProductsCount;
    }

    private final Object syncRoot = new Object();

    public boolean TryBuy(int neededCount)
    {
        synchronized (syncRoot)
        {
            if (neededCount > CurrentProductsCount)
                return false;

            CurrentProductsCount -= neededCount;
            return true;
        }
    }

    public String getName() {
        return Name;
    }
}
