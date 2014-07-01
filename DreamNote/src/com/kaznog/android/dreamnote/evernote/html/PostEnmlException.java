package com.kaznog.android.dreamnote.evernote.html;


/**
 * <p>General Html Analyze runtime exception.</p>
 */
public class PostEnmlException extends RuntimeException {

    /**
	 *
	 */
	private static final long serialVersionUID = -6910339285190316257L;

	public PostEnmlException() {
        this("DreamNote Html Analyze expression!");
    }

    public PostEnmlException(Throwable cause) {
        super(cause);
    }

    public PostEnmlException(String message) {
        super(message);
    }

    public PostEnmlException(String message, Throwable cause) {
        super(message, cause);
    }

}