package nextstep.di.factory;

import com.google.common.collect.Maps;
import nextstep.exception.BeanFactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BeanFactory {
    private static final Logger logger = LoggerFactory.getLogger(BeanFactory.class);

    private Set<Class<?>> preInstanticateBeans;

    private Map<Class<?>, Object> beans = Maps.newHashMap();

    public BeanFactory(Set<Class<?>> preInstanticateBeans) {
        this.preInstanticateBeans = preInstanticateBeans;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        return (T) beans.get(requiredType);
    }

    public void initialize() {
        preInstanticateBeans.forEach(preInstanticateBean -> {
            Object bean = createBean(preInstanticateBean);
            beans.put(preInstanticateBean, bean);
        });
    }

    private Object createBean(final Class<?> preInstanticateBean) {
        Optional<Constructor<?>> injectedConstructor = BeanFactoryUtils.getInjectedConstructor(preInstanticateBean);

        return injectedConstructor.map(this::createInjectedBean)
                .orElseGet(() -> createConcreteClassBean(preInstanticateBean));

    }

    private Object createInjectedBean(final Constructor<?> injectedConstructor) {
        try {
            Class<?>[] parameterTypes = injectedConstructor.getParameterTypes();
            List<Object> parameters = createParameters(parameterTypes);

            return injectedConstructor.newInstance(parameters.toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            logger.error(e.getMessage());
            throw new BeanFactoryException(e);
        }
    }

    private List<Object> createParameters(final Class<?>[] parameterTypes) {
        return Stream.of(parameterTypes)
                .map(this::createBean)
                .collect(Collectors.toList());
    }

    private Object createConcreteClassBean(final Class<?> preInstanticateBean) {
        try {
            Class<?> concreteClass = BeanFactoryUtils.findConcreteClass(preInstanticateBean, preInstanticateBeans);
            return concreteClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(e.getMessage());
            throw new BeanFactoryException(e);
        }
    }
}
