package io.omixer.rpm

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property

/**
 * Collects every log event as "LEVEL message" while installed on the root logger.
 */
class LogCapture extends AbstractAppender {

	final List<String> lines = Collections.synchronizedList([])

	LogCapture() {
		super("capture", null, null, true, Property.EMPTY_ARRAY)
	}

	@Override
	void append(LogEvent event) {
		lines << "${event.getLevel().name()} ${event.getMessage().getFormattedMessage()}".toString()
	}

	LogCapture install() {
		def context = (LoggerContext) LogManager.getContext(false)
		start()
		context.getConfiguration().addAppender(this)
		context.getConfiguration().getRootLogger().addAppender(this, null, null)
		context.updateLoggers()
		this
	}

	void uninstall() {
		def context = (LoggerContext) LogManager.getContext(false)
		context.getConfiguration().getRootLogger().removeAppender(getName())
		context.updateLoggers()
		stop()
	}
}
