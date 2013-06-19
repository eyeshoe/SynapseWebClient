package org.sagebionetworks.web.server.markdownparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeadingParser extends BasicMarkdownElementParser  {
	Pattern p1;
	public static final String HEADING_REGEX = "((#{1,6})\\s*(.*))";
	boolean isHeading;
	
	@Override
	public void init() {
		p1 = Pattern.compile(HEADING_REGEX, Pattern.DOTALL);
		isHeading = false;
	}

	@Override
	public String processLine(String line) {
		Matcher m = p1.matcher(line);
		isHeading = m.matches();
		if (isHeading) {
			//looks like a heading
			String hashes = m.group(2);
            String headingText = m.group(3);
            int level = hashes.length();
            String tag = "h" + level;
            return "<" + tag + ">" + headingText + "</" + tag + ">";
		}
		return line;
	}
	
	@Override
	public boolean isInMarkdownElement() {
		return isHeading;
	}
	
	@Override
	public boolean isBlockElement() {
		return true;
	}
}
