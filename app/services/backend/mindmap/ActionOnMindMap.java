package services.backend.mindmap;

import play.libs.F.Promise;

abstract class ActionOnMindMap<T> {
	public abstract Promise<T> perform(Promise<Object> promise);
	
	/**
	 * Possibility to react on an exception. Similar to recover at promise
	 * @param t
	 * @return Promise<T> or null, when unhandled
	 */
	public Promise<T> handleException(Throwable t) {
		return null;
	}
}