package grails.plugin.cache

import groovy.transform.CompileStatic

/**
 * A runtime Cache Exception.
 * <p/>
 * The handler provides a key if it is available. A convention that should be followed in exception messages is
 * to include in the message "key keyValue" e.g. "key 1234" so that keys can be parsed out of exception messages.
 * <p/>
 *
 * @author James Kleeh
 *
 */
@CompileStatic
class CacheException extends RuntimeException {

    /**
     * Constructor for the CacheException object.
     */
    CacheException() {
        super()
    }

    /**
     * Constructor for the CacheException object.
     * @param message the exception detail message
     */
    CacheException(String message) {
        super(message)
    }

    /**
     * Constructs a new CacheException with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     * {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.2.4
     */
    CacheException(String message, Throwable cause) {
        super(message, cause)
    }

    /** Constructs a new CacheException with the specified cause and a
     * detail message of <tt>(cause==null ? null : cause.toString())</tt>
     * (which typically contains the class and detail message of
     * <tt>cause</tt>).  This constructor is useful for runtime exceptions
     * that are little more than wrappers for other throwables.
     * * @param  cause the cause (which is saved for later retrieval by the
     * {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.2.4
     */
    CacheException(Throwable cause) {
        super(cause)
    }
}