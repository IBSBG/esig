/**
 * © Nowina Solutions, 2015-2015
 * <p>
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 * <p>
 * http://ec.europa.eu/idabc/eupl5
 * <p>
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu.view.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import lu.nowina.nexu.NexuException;
import lu.nowina.nexu.api.Feedback;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.generic.DebugHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class ProvideFeedbackController extends AbstractFeedbackUIOperationController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvideFeedbackController.class);

    @FXML
    private Button ok;

    @FXML
    private Button cancel;

    @FXML
    private Label message;

    @FXML
    private TextArea userComment;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        ok.setOnAction(e -> {
            DebugHelper dh = new DebugHelper();
            Feedback feedback = null;
            try {
                feedback = dh.processError(new NexuException());
            } catch (IOException | JAXBException ex) {
                LOGGER.warn(ex.getMessage(), ex);
            }
            new Thread(() -> {
                try {
                    Desktop.getDesktop().browse(new URI(getAppConfig().getTicketUrl()));
                } catch (IOException | URISyntaxException ioe) {
                    LOGGER.error(ioe.getMessage());
                }
            }).start();
            signalEnd(feedback);
        });
        cancel.setOnAction(e -> signalUserCancel());
    }

    @Override
    protected void doInit(Object... params) {
        StageHelper.getInstance().setTitle(getApplicationName(), "feedback.title");
        Platform.runLater(() ->
                message.setText(MessageFormat.format(
                        StageHelper.getBundle().getString("feedback.message"),
                        StageHelper.getBundle().getString("button.report.incident"), getApplicationName())));
    }

}
