package act.inject.param;

import act.Destroyable;
import act.app.AppServiceBase;
import act.inject.DependencyInjector;
import act.util.ActContext;
import act.util.DestroyableBase;
import act.util.SingletonBase;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class ProvidedValueLoader extends DestroyableBase implements ParamValueLoader {
    private DependencyInjector<?> injector;
    private Class type;
    private Object singleton;
    private ProvidedValueLoader(Class type, DependencyInjector<?> injector) {
        if (AppServiceBase.class.isAssignableFrom(type)
                || SingletonBase.class.isAssignableFrom(type)
                || type.isAnnotationPresent(Singleton.class)
                || type.isAnnotationPresent(ApplicationScoped.class)) {
            singleton = injector.get(type);
        } else {
            this.type = type;
        }
        this.injector = injector;
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        if (context.getClass().equals(type)) {
            return context;
        } else if (null != singleton) {
            return singleton;
        } else {
            return injector.get(type);
        }
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroy(singleton);
        singleton = null;
        injector = null;
        lookup.clear();
    }

    private static ConcurrentMap<Class, ProvidedValueLoader> lookup = new ConcurrentHashMap<>();
    static ProvidedValueLoader get(Class type, DependencyInjector<?> injector) {
        ProvidedValueLoader loader = lookup.get(type);
        if (null == loader) {
            loader = new ProvidedValueLoader(type, injector);
            lookup.putIfAbsent(type, loader);
        }
        return loader;
    }
}
