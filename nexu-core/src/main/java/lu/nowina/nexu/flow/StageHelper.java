package lu.nowina.nexu.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.apache.commons.lang.StringUtils.isBlank;

public class StageHelper {

	private static StageHelper instance;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StageHelper.class);

	private String title;

	private static ResourceBundle bundle = ResourceBundle.getBundle("bundles/nexu", Locale.ENGLISH);;
	
	private StageHelper() {
	}

	public static synchronized StageHelper getInstance() {
		if (instance == null) {
			synchronized (StageHelper.class) {
				if (instance == null) {
					instance = new StageHelper();
				}
			}
		}
		return instance;
	}

	public String getTitle() {
		return title;
	}

	public static ResourceBundle getBundle() {
		return bundle;
	}

	public static void setBundle(ResourceBundle bundle) {
		StageHelper.bundle = bundle;
	}

	public void setTitle(final String applicationName, String bundleKey) {
		try {
			title = bundle.getString(bundleKey);
		} catch (Exception e) {
			title = "";
		}
		if(!isBlank(applicationName) && !isBlank(title)) {
			this.title = applicationName + " - " + title;
		} else if(isBlank(applicationName)) {
			this.title = title;
		} else if(isBlank(title)) {
			this.title = applicationName;
		} else {
			this.title = "";
		}
	}
}
