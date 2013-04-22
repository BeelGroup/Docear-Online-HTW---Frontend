package base;

import java.lang.reflect.Method;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

public class TestRunner extends Runner {
	private Class testClass;

	public TestRunner(Class testClass){
		this.testClass = testClass;
	}

	@Override
	public Description getDescription() {
		Description spec = Description.createSuiteDescription(this.testClass.getName(),
                this.testClass.getClass().getAnnotations());
		return spec;
	}

	@Override
	public void run(RunNotifier notifier) {
		for (Method m : testClass.getMethods()){
			if (m.isAnnotationPresent(Test.class) && !m.isAnnotationPresent(Ignore.class)){
				notifier.fireTestStarted(Description.createTestDescription(m.getClass(), m.getName()));
			}
		}
	}
}
