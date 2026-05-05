import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.media.AudioClip;

// ── RichTextFX ────────────────────────────────────────────────────────────────
// Requires richtextfx-fat-0.11.2.jar on the classpath.
// Fat JAR already bundles ReactFX, UndoFX, and Flowless — no extra JARs needed.
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.flowless.VirtualizedScrollPane;
import java.util.List;


public class Main extends Application {

    private static final String C_BG                 = "#f7fafc";
    private static final String C_SURFACE_LOW        = "#eff4f7";
    private static final String C_SURFACE_HIGH       = "#dfeaef";
    private static final String C_SURFACE_HIGHEST    = "#d7e5eb";
    private static final String C_SURFACE_LOWEST     = "#ffffff";
    private static final String C_PRIMARY            = "#3b6090";
    private static final String C_PRIMARY_CONTAINER  = "#d4e3ff";
    private static final String C_ON_SURFACE         = "#283439";
    private static final String C_ON_SURFACE_VAR     = "#546166";
    private static final String C_TERTIARY           = "#5b5d78";
    private static final String C_TERTIARY_CONTAINER = "#ddddfe";
    private static final String C_OUTLINE            = "#707d82";
    private static final String C_OUTLINE_VAR        = "#a7b4ba";
    private static final String C_ERROR              = "#9f403d";
    private static final String C_ERROR_CONTAINER    = "#fe8983";


    private static final String CHAR_BASE_CSS =
            "-fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-fill: " + C_ON_SURFACE + ";";

    private Stage primaryStage;

    private static String bg(String h) { return "-fx-background-color:" + h + ";"; }
    private static String fg(String h) { return "-fx-text-fill:" + h + ";"; }



    private String charCss(boolean isBold, boolean isItalic) {
        StringBuilder css = new StringBuilder();

        css.append("-fx-fill: ").append(C_ON_SURFACE).append("; ");
        css.append("-fx-font-family: 'System'; ");
        css.append("-fx-font-size: 15px; ");

        if (isBold) {
            css.append("-fx-font-weight: bold; ");
        } else {
            css.append("-fx-font-weight: normal; ");
        }

        if (isItalic) {
            css.append("-fx-font-style: italic; ");
        } else {
            css.append("-fx-font-style: normal; ");
        }

        return css.toString();
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("TextDoc");
        stage.setWidth(1150);
        stage.setHeight(740);
        showWelcome();
        stage.show();
    }


    public void handleRemoteOperation(String jsonMessage) {
        Platform.runLater(() -> {
            try {
                Gson gson = new Gson();
                JsonObject op = gson.fromJson(jsonMessage, JsonObject.class);
                String type = op.get("type").getAsString();


                if (type.equals("JOIN_ACCEPTED")) {
                    myRole = op.get("role").getAsString();
                    historyReplayDone = false;
                    Platform.runLater(() -> showEditor());
                    return;
                }
                if (type.equals("HISTORY_START")) {
                    myCrdt = new DocumentCRDT(myUserId);
                    return;
                }

                if (type.equals("HISTORY_END")) {
                    historyReplayDone = true;
                    Platform.runLater(() -> refreshDisplay(0));
                    return;
                }

                if (type.equals("JOIN_REJECTED")) {
                    String reason = op.get("reason").getAsString();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Connection Rejected");
                        alert.setHeaderText("Invalid Room Code");
                        alert.setContentText(reason);
                        alert.showAndWait();
                    });
                    return;
                }

                if (type.equals("USER_JOINED")) {
                    String newUserId = op.get("userId").getAsString();
                    connectedUsers.put(newUserId, new RemoteUser(newUserId, "User-" + newUserId.substring(0, 2)));
                    refreshUserList();
                    return;
                }

                if (type.equals("USER_LEFT")) {
                    String leftUserId = op.get("userId").getAsString();
                    connectedUsers.remove(leftUserId);
                    refreshUserList();
                    return;
                }

                String siteId = op.get("siteId").getAsString();

                if (!siteId.equals(myUserId) && !connectedUsers.containsKey(siteId)) {
                    connectedUsers.put(siteId, new RemoteUser(siteId, siteId));
                    refreshUserList();
                }

                int clock = op.get("clock").getAsInt();

                if (type.equals("FORMAT")) {
                    boolean bold   = op.get("bold").getAsBoolean();
                    boolean italic = op.get("italic").getAsBoolean();
                    myCrdt.applyBold(siteId, clock, bold);
                    myCrdt.applyItalic(siteId, clock, italic);
                    if (editor != null) refreshDisplay(editor.getCaretPosition());
                    return;
                }

                if (type.equals("INSERT")) {
                    char value = op.get("value").getAsString().charAt(0);
                    String afterSiteId = (op.has("afterSiteId") && !op.get("afterSiteId").isJsonNull())
                            ? op.get("afterSiteId").getAsString() : null;
                    int afterClock = op.get("afterClock").getAsInt();

                    CharacterNode incomingNode = new CharacterNode(siteId, clock, value, afterSiteId, afterClock);
                    myCrdt.remoteInsert(incomingNode);

                } else if (type.equals("DELETE")) {
                    myCrdt.remoteDelete(siteId, clock);
                }

                int oldCursorIndex = editor.getCaretPosition();
                int newCursorIndex = oldCursorIndex;
                int remoteChangeIndex = myCrdt.getVisibleIndex(siteId, clock);

                if (remoteChangeIndex != -1) {
                    if (type.equals("INSERT") && remoteChangeIndex <= oldCursorIndex) newCursorIndex++;
                    else if (type.equals("DELETE") && remoteChangeIndex < oldCursorIndex) newCursorIndex--;
                }

                refreshDisplay(newCursorIndex);

            } catch (Exception e) {
                System.err.println("JSON Error: " + e.getMessage());
            }
        });
    }


    private void showWelcome() {
        BorderPane root = new BorderPane();
        root.setStyle(bg(C_BG));
        root.setTop(buildWelcomeHeader());
        root.setCenter(buildWelcomeMain());
        root.setBottom(buildStatusBar());
        primaryStage.setScene(new Scene(root));
    }

    private HBox buildWelcomeHeader() {
        HBox header = new HBox();
        header.setStyle(bg(C_SURFACE_LOW));
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        Label logo = new Label("TextDoc");
        logo.setStyle(fg(C_PRIMARY) + "-fx-font-size:20px;-fx-font-weight:bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(32, navLink("Editor", true), navLink("Projects", false));
        nav.setAlignment(Pos.CENTER);

        HBox icons = new HBox(8, iconBtn("?"), iconBtn("⚙"));
        icons.setAlignment(Pos.CENTER);
        icons.setPadding(new Insets(0, 0, 0, 24));

        header.getChildren().addAll(logo, spacer, nav, icons);
        return header;
    }

    private HBox buildWelcomeMain() {
        HBox main = new HBox();
        VBox left = buildConnectPanel();
        HBox.setHgrow(left, Priority.ALWAYS);
        main.setAlignment(Pos.CENTER);
        main.getChildren().add(left);
        return main;
    }

    private VBox buildConnectPanel() {
        VBox panel = new VBox(32);
        panel.setPadding(new Insets(72, 80, 72, 80));
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setStyle(bg(C_BG));

        Label hello = new Label("Hello.");
        hello.setStyle(fg(C_ON_SURFACE) + "-fx-font-size:56px;-fx-font-weight:900;");

        Label sub = new Label("Welcome back to TextDoc. Connect to a remote\nsession or resume your local editorial work.");
        sub.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:15px;");

        VBox serverAddressBox = formField("Server Address", "ws://your-server:port", "ws://localhost:8888");
        VBox roomCodeBox      = formField("Room Code", "Enter 6-character code...", "");

        TextField serverAddressInput = (TextField) serverAddressBox.getChildren().get(1);
        TextField roomCodeInput      = (TextField) roomCodeBox.getChildren().get(1);

        VBox form = new VBox(24);
        form.setMaxWidth(420);
        form.getChildren().addAll(serverAddressBox, roomCodeBox);

        String btnBase = "-fx-background-color:linear-gradient(to right,#3b6090,#d4e3ff);"
                + fg(C_ON_SURFACE) + "-fx-padding:13 38 13 38;-fx-background-radius:12;-fx-cursor:hand;";
        Button connectBtn = new Button("Connect  →");
        connectBtn.setFont(Font.font("System", FontWeight.BOLD, 15));
        connectBtn.setStyle(btnBase);
        connectBtn.setOnMouseEntered(e -> connectBtn.setStyle(btnBase + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),8,0,0,2);"));
        connectBtn.setOnMouseExited(e  -> connectBtn.setStyle(btnBase));

        panel.getChildren().addAll(hello, sub, form, connectBtn);
        connectBtn.setOnAction(e -> {
            try {
                if (webSocketClient != null && webSocketClient.isOpen()) {
                    webSocketClient.close();
                }
                resetSession();

                String targetServer = serverAddressInput.getText();
                String enteredCode  = roomCodeInput.getText().trim().toUpperCase();

                webSocketClient = new CollabClient(targetServer, message -> handleRemoteOperation(message));
                webSocketClient.connect();

                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        String joinMsg = String.format(
                                "{\"type\":\"JOIN\",\"code\":\"%s\",\"siteId\":\"%s\"}",
                                enteredCode, myUserId);
                        webSocketClient.send(joinMsg);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();

            } catch (Exception ex) {
                System.err.println("Failed to build the client!");
                ex.printStackTrace();
            }
        });
        return panel;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox();
        bar.setStyle(bg(C_SURFACE_HIGH));
        bar.setPadding(new Insets(6, 24, 6, 24));
        bar.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(4, Color.web(C_ERROR));
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,         e -> dot.setOpacity(1.0)),
                new KeyFrame(Duration.millis(700),  e -> dot.setOpacity(0.3)),
                new KeyFrame(Duration.millis(1400), e -> dot.setOpacity(1.0)));
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();

        Label offline  = new Label("OFFLINE");
        offline.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:9px;-fx-font-weight:bold;");
        Rectangle divLine = new Rectangle(1, 12, Color.web(C_OUTLINE_VAR));
        divLine.setOpacity(0.3);
        Label version  = new Label("v1.0.4-stable");
        version.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:9px;");

        HBox left = new HBox(8, dot, offline, divLine, version);
        left.setAlignment(Pos.CENTER_LEFT);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Label sync     = new Label("Sync Enabled");
        sync.setStyle(fg(C_TERTIARY) + "-fx-font-size:9px;-fx-font-weight:600;");
        Label encoding = new Label("UTF-8");
        encoding.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:9px;");

        HBox right = new HBox(16, sync, encoding);
        right.setAlignment(Pos.CENTER_RIGHT);

        bar.getChildren().addAll(left, sp, right);
        return bar;
    }


    private void showEditor() {
        BorderPane root = new BorderPane();
        root.setStyle(bg(C_BG));
        root.setTop(buildEditorTop());
        root.setCenter(buildEditorCenter());
        primaryStage.setScene(new Scene(root));
    }

    private VBox buildEditorTop() {
        HBox header = new HBox();
        header.setStyle(bg(C_BG));
        header.setPadding(new Insets(10, 24, 10, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        Label logo = new Label("TextDoc");
        logo.setStyle(fg(C_ON_SURFACE) + "-fx-font-size:18px;-fx-font-weight:bold;");
        logo.setCursor(Cursor.HAND);
        logo.setOnMouseClicked(e -> playSound());

        Rectangle divLine = new Rectangle(1, 16, Color.web(C_OUTLINE_VAR));
        divLine.setOpacity(0.3);

        Label tag = new Label("✏  COLLABORATIVE EDITOR");
        tag.setStyle(fg(C_PRIMARY) + "-fx-font-size:10px;-fx-font-weight:bold;");

        HBox leftGroup = new HBox(10, logo, divLine, tag);
        leftGroup.setAlignment(Pos.CENTER_LEFT);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        HBox searchBox = new HBox(6);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle(bg(C_SURFACE_LOW) + "-fx-background-radius:8;-fx-padding:6 12 6 12;");
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size:12px;-fx-opacity:0.6;");
        TextField searchField = new TextField();
        searchField.setPromptText("Search project...");
        searchField.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;"
                + fg(C_ON_SURFACE_VAR) + "-fx-font-size:13px;-fx-pref-width:160;");
        searchBox.getChildren().addAll(searchIcon, searchField);

        header.getChildren().addAll(leftGroup, sp);

        HBox toolbar = new HBox();
        toolbar.setStyle(bg(C_SURFACE_LOW));
        toolbar.setPadding(new Insets(10, 24, 10, 24));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(5, Color.web(C_ERROR));
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,         e -> dot.setOpacity(1.0)),
                new KeyFrame(Duration.millis(700),  e -> dot.setOpacity(0.3)),
                new KeyFrame(Duration.millis(1400), e -> dot.setOpacity(1.0)));
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();

        Label disconnected = new Label("Disconnected");
        disconnected.setStyle(fg(C_ON_SURFACE) + "-fx-font-size:13px;-fx-font-weight:600;");
        Label wsAddr = new Label("• ws://localhost:8080");
        wsAddr.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:11px;-fx-opacity:0.6;");

        HBox statusGrp = new HBox(6, dot, disconnected);
        statusGrp.setAlignment(Pos.CENTER_LEFT);

        Region tsp = new Region(); HBox.setHgrow(tsp, Priority.ALWAYS);

        boldBtn = fmtBtn("B");
        boldBtn.setOnMouseExited(e -> updateFmtButtonStates(editor != null ? editor.getCaretPosition() : 0));
        boldBtn.setOnAction(e -> applyFormattingToSelection(true, false));

        italicBtn = fmtBtn("I");
        italicBtn.setOnMouseExited(e -> updateFmtButtonStates(editor != null ? editor.getCaretPosition() : 0));
        italicBtn.setOnAction(e -> applyFormattingToSelection(false, true));

        HBox fmtBar = new HBox(2);
        fmtBar.setAlignment(Pos.CENTER);
        fmtBar.setStyle(bg(C_SURFACE_HIGH) + "-fx-background-radius:20;-fx-padding:4;");
        fmtBar.getChildren().addAll(boldBtn, italicBtn,
                new Separator(Orientation.VERTICAL), fmtBtn("≡"), fmtBtn("🔗"));

        toolbar.getChildren().addAll(statusGrp, tsp, fmtBar);

        return new VBox(header, toolbar);
    }

    private HBox buildEditorCenter() {
        HBox center = new HBox();

        VBox editorArea = new VBox(16);
        editorArea.setPadding(new Insets(56, 80, 56, 80));
        editorArea.setStyle(bg(C_BG));
        HBox.setHgrow(editorArea, Priority.ALWAYS);

        Label docTitle = new Label("Draft_01");
        docTitle.setStyle(fg(C_ON_SURFACE) + "-fx-font-size:38px;-fx-font-weight:900;-fx-opacity:0.10;");

        editor = new InlineCssTextArea();
        editor.setWrapText(true);
        editor.setEditable("editor".equals(myRole));


        editor.setStyle(bg(C_BG)
                + "-fx-border-color: transparent;"
                + "-fx-background-radius: 0;"
                + "-fx-padding: 0;");

        VirtualizedScrollPane<InlineCssTextArea> editorScroll = new VirtualizedScrollPane<>(editor);
        VBox.setVgrow(editorScroll, Priority.ALWAYS);



        editor.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            String characterStr = event.getCharacter();
            if (!"editor".equals(myRole)) { event.consume(); return; }

            if (characterStr.isEmpty() || characterStr.charAt(0) < 32) return;

            int cursorIndex = editor.getCaretPosition();
            String afterSiteId = null;
            int afterClock = 0;

            if (cursorIndex > 0) {
                CharacterNode prevNode = myCrdt.getVisibleNodeAt(cursorIndex - 1);
                if (prevNode != null) {
                    afterSiteId = prevNode.siteId;
                    afterClock  = prevNode.clock;
                }
            }

            char letter = characterStr.charAt(0);

            CharacterNode newNode = myCrdt.localInsert(letter, afterSiteId, afterClock);

            if (boldActive)   myCrdt.applyBold(newNode.siteId, newNode.clock, true);
            if (italicActive) myCrdt.applyItalic(newNode.siteId, newNode.clock, true);

            String safeValue = String.valueOf(newNode.value);
            if (newNode.value == '\\') safeValue = "\\\\";
            else if (newNode.value == '"') safeValue = "\\\"";

            String afterIdJson = (newNode.afterSiteId == null) ? "null" : "\"" + newNode.afterSiteId + "\"";
            String insertMessage = String.format(
                    "{\"type\": \"INSERT\", \"siteId\": \"%s\", \"clock\": %d, \"value\": \"%s\", \"afterSiteId\": %s, \"afterClock\": %d}",
                    newNode.siteId, newNode.clock, safeValue, afterIdJson, newNode.afterClock
            );

            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.send(insertMessage);
            }

            if (boldActive || italicActive) {
                String formatMessage = String.format(
                        "{\"type\": \"FORMAT\", \"siteId\": \"%s\", \"clock\": %d, \"bold\": %b, \"italic\": %b}",
                        newNode.siteId, newNode.clock, newNode.isBold, newNode.isItalic
                );
                if (webSocketClient != null && webSocketClient.isOpen()) {
                    webSocketClient.send(formatMessage);
                }
            }

            event.consume();
            refreshDisplay(cursorIndex + 1);
        });

        editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!"editor".equals(myRole)) { event.consume(); return; }

            if (event.getCode() == KeyCode.ENTER) {
                int cursorIndex = editor.getCaretPosition();
                String afterSiteId = null;
                int afterClock = 0;

                if (cursorIndex > 0) {
                    CharacterNode prevNode = myCrdt.getVisibleNodeAt(cursorIndex - 1);
                    if (prevNode != null) {
                        afterSiteId = prevNode.siteId;
                        afterClock  = prevNode.clock;
                    }
                }

                CharacterNode newNode = myCrdt.localInsert('\n', afterSiteId, afterClock);

                String afterIdJson = (newNode.afterSiteId == null) ? "null" : "\"" + newNode.afterSiteId + "\"";
                String insertMessage = String.format(
                        "{\"type\": \"INSERT\", \"siteId\": \"%s\", \"clock\": %d, \"value\": \"%s\", \"afterSiteId\": %s, \"afterClock\": %d}",
                        newNode.siteId, newNode.clock, "\\n", afterIdJson, newNode.afterClock
                );

                if (webSocketClient != null && webSocketClient.isOpen()) {
                    webSocketClient.send(insertMessage);
                }

                event.consume();
                refreshDisplay(cursorIndex + 1);

            } else if (event.getCode() == KeyCode.BACK_SPACE) {
                int cursorIndex = editor.getCaretPosition();

                if (cursorIndex > 0) {
                    CharacterNode nodeToDelete = myCrdt.getVisibleNodeAt(cursorIndex - 1);

                    if (nodeToDelete != null) {
                        myCrdt.localDelete(nodeToDelete.siteId, nodeToDelete.clock);

                        String deleteMessage = String.format(
                                "{\"type\": \"DELETE\", \"siteId\": \"%s\", \"clock\": %d}",
                                nodeToDelete.siteId, nodeToDelete.clock
                        );

                        if (webSocketClient != null && webSocketClient.isOpen()) {
                            webSocketClient.send(deleteMessage);
                        }
                    }

                    event.consume();
                    refreshDisplay(cursorIndex - 1);
                }
            }
        });

        editor.caretPositionProperty().addListener((obs, oldPos, newPos) ->
                updateFmtButtonStates(newPos.intValue()));



        editorArea.getChildren().addAll(docTitle, editorScroll);

        VBox sidebar = new VBox();
        sidebar.setPrefWidth(272);
        sidebar.setMinWidth(272);
        sidebar.setMaxWidth(272);
        sidebar.setStyle(bg(C_SURFACE_LOW)
                + "-fx-background-radius:12 0 0 12;"
                + "-fx-effect:dropshadow(gaussian,rgba(40,52,57,0.08),16,0,-4,0);");
        sidebar.setPadding(new Insets(24, 0, 24, 0));

        VBox sideHdr = new VBox(4);
        sideHdr.setPadding(new Insets(0, 24, 24, 24));
        Label activeTitle = new Label("Active Users");
        activeTitle.setStyle(fg(C_ON_SURFACE) + "-fx-font-size:17px;-fx-font-weight:bold;");
        Label roomInfo = new Label("Room 1024 • Connected");
        roomInfo.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:11px;-fx-opacity:0.6;");
        sideHdr.getChildren().add(activeTitle);

        VBox userList = new VBox(2);
        VBox.setVgrow(userList, Priority.ALWAYS);

        this.userListContainer = userList;

        refreshUserList();

        VBox bottomActions = new VBox(0);
        bottomActions.setPadding(new Insets(16, 16, 0, 16));

        Button inviteBtn = new Button("➕  Invite Editor");
        inviteBtn.setMaxWidth(Double.MAX_VALUE);
        String invBase = bg(C_SURFACE_HIGHEST) + fg(C_ON_SURFACE_VAR)
                + "-fx-font-size:12px;-fx-font-weight:bold;"
                + "-fx-padding:12 0 12 0;-fx-background-radius:12;-fx-cursor:hand;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),4,0,0,1);";
        inviteBtn.setStyle(invBase);
        inviteBtn.setOnMouseEntered(e -> inviteBtn.setStyle(bg(C_SURFACE_LOWEST) + fg(C_ON_SURFACE_VAR)
                + "-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:12 0 12 0;-fx-background-radius:12;-fx-cursor:hand;"));
        inviteBtn.setOnMouseExited(e  -> inviteBtn.setStyle(invBase));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:" + C_OUTLINE_VAR + ";-fx-opacity:0.1;");
        VBox.setMargin(sep, new Insets(16, 0, 0, 0));

        HBox helpRow   = sidebarAction("❓", "Help",   false);
        HBox logoutRow = sidebarAction("🚪", "Logout", true);
        logoutRow.setOnMouseClicked(e -> showWelcome());

        bottomActions.getChildren().add(logoutRow);
        sidebar.getChildren().addAll(sideHdr, userList, bottomActions);

        center.getChildren().addAll(editorArea, sidebar);
        return center;
    }




    private void refreshDisplay(int targetCaret) {
        if (editor == null) return;

        List<CharacterNode> nodes = myCrdt.getVisibleNodes();

        StringBuilder sb = new StringBuilder();
        for (CharacterNode n : nodes) sb.append(n.value);
        String newText = sb.toString();

        editor.replaceText(0, editor.getLength(), newText);

        for (int i = 0; i < nodes.size(); i++) {
            CharacterNode n = nodes.get(i);
            editor.setStyle(i, i + 1, charCss(n.isBold, n.isItalic));
        }

        int clampedCaret = Math.min(targetCaret, newText.length());
        editor.moveTo(clampedCaret);
    }

    /**
     * Applies bold OR italic to the currently selected text range.
     *
     * • With a selection  → toggle that property on every selected character
     *   (Word-style: if ALL chars are already bold → un-bold; otherwise bold all)
     *   and broadcast FORMAT messages.
     * • Without a selection → flip the "typing mode" flag so the next typed
     *   characters will be bold/italic.
     *
     * @param isBoldToggle   true when the Bold   button was clicked
     * @param isItalicToggle true when the Italic button was clicked
     */
    private void applyFormattingToSelection(boolean isBoldToggle, boolean isItalicToggle) {
        if (editor == null) return;

        IndexRange sel = editor.getSelection();

        if (sel.getLength() == 0) {
            if (isBoldToggle)   boldActive   = !boldActive;
            if (isItalicToggle) italicActive = !italicActive;
            updateFmtButtonStates(editor.getCaretPosition());
            return;
        }

        List<CharacterNode> allNodes = myCrdt.getVisibleNodes();
        int from = sel.getStart();
        int to   = Math.min(sel.getEnd(), allNodes.size());
        if (from >= to) return;

        List<CharacterNode> selected = allNodes.subList(from, to);

        if (isBoldToggle) {
            boolean allBold = selected.stream().allMatch(n -> n.isBold);
            boolean newBold = !allBold;
            for (CharacterNode node : selected) {
                myCrdt.applyBold(node.siteId, node.clock, newBold);
                String msg = String.format(
                        "{\"type\": \"FORMAT\", \"siteId\": \"%s\", \"clock\": %d, \"bold\": %b, \"italic\": %b}",
                        node.siteId, node.clock, newBold, node.isItalic);
                if (webSocketClient != null && webSocketClient.isOpen()) webSocketClient.send(msg);
            }
        }

        if (isItalicToggle) {
            boolean allItalic = selected.stream().allMatch(n -> n.isItalic);
            boolean newItalic = !allItalic;
            for (CharacterNode node : selected) {
                myCrdt.applyItalic(node.siteId, node.clock, newItalic);
                String msg = String.format(
                        "{\"type\": \"FORMAT\", \"siteId\": \"%s\", \"clock\": %d, \"bold\": %b, \"italic\": %b}",
                        node.siteId, node.clock, node.isBold, newItalic);
                if (webSocketClient != null && webSocketClient.isOpen()) webSocketClient.send(msg);
            }
        }

        refreshDisplay(editor.getCaretPosition());
        updateFmtButtonStates(editor.getCaretPosition());
    }


    private void updateFmtButtonStates(int caretPos) {
        if (boldBtn == null || italicBtn == null) return;

        if (caretPos > 0) {
            CharacterNode node = myCrdt.getVisibleNodeAt(caretPos - 1);
            if (node != null) {
                boldActive   = node.isBold;
                italicActive = node.isItalic;
            }
        }

        String activeStyle = bg(C_PRIMARY_CONTAINER) + fg(C_PRIMARY)
                + "-fx-font-size:14px;-fx-font-weight:bold;-fx-cursor:hand;"
                + "-fx-padding:6 10 6 10;-fx-background-radius:20;";
        String inactiveStyle = "-fx-background-color:transparent;" + fg(C_ON_SURFACE_VAR)
                + "-fx-font-size:14px;-fx-font-weight:bold;-fx-cursor:hand;"
                + "-fx-padding:6 10 6 10;-fx-background-radius:20;";

        boldBtn.setStyle(boldActive     ? activeStyle : inactiveStyle);
        italicBtn.setStyle(italicActive ? activeStyle : inactiveStyle);
    }


    private Label navLink(String text, boolean active) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 16));
        lbl.setPadding(new Insets(4, 0, 4, 0));
        lbl.setCursor(Cursor.HAND);
        if (active)
            lbl.setStyle(fg(C_PRIMARY) + "-fx-border-color:transparent transparent " + C_PRIMARY + " transparent;-fx-border-width:0 0 2 0;");
        else {
            lbl.setStyle(fg(C_ON_SURFACE));
            lbl.setOnMouseEntered(e -> lbl.setStyle(fg(C_ON_SURFACE) + bg(C_SURFACE_HIGH)));
            lbl.setOnMouseExited(e  -> lbl.setStyle(fg(C_ON_SURFACE)));
        }
        return lbl;
    }

    private StackPane iconBtn(String symbol) {
        Label icon = new Label(symbol);
        icon.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:16px;");
        StackPane btn = new StackPane(icon);
        btn.setPadding(new Insets(8));
        btn.setCursor(Cursor.HAND);
        btn.setStyle("-fx-background-radius:50;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-radius:50;" + bg(C_SURFACE_HIGH)));
        btn.setOnMouseExited(e  -> btn.setStyle("-fx-background-radius:50;"));
        return btn;
    }

    private Button fmtBtn(String label) {
        String base = "-fx-background-color:transparent;" + fg(C_ON_SURFACE_VAR)
                + "-fx-font-size:14px;-fx-font-weight:bold;-fx-cursor:hand;"
                + "-fx-padding:6 10 6 10;-fx-background-radius:20;";
        Button btn = new Button(label);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(bg(C_SURFACE_LOWEST) + fg(C_ON_SURFACE_VAR)
                + "-fx-font-size:14px;-fx-font-weight:bold;-fx-cursor:hand;"
                + "-fx-padding:6 10 6 10;-fx-background-radius:20;"));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private VBox formField(String labelText, String placeholder, String defaultVal) {
        VBox field = new VBox(6);
        Label lbl = new Label(labelText.toUpperCase());
        lbl.setStyle(fg(C_TERTIARY) + "-fx-font-size:10px;-fx-font-weight:bold;");
        TextField tf = new TextField(defaultVal);
        tf.setPromptText(placeholder);
        tf.setFont(Font.font("Monospaced", 14));
        String base = bg(C_SURFACE_HIGHEST) + fg(C_ON_SURFACE)
                + "-fx-border-color:transparent transparent " + C_OUTLINE_VAR + " transparent;"
                + "-fx-border-width:0 0 2 0;-fx-background-radius:0;-fx-padding:10 0 10 0;";
        String focused = bg(C_SURFACE_HIGHEST) + fg(C_ON_SURFACE)
                + "-fx-border-color:transparent transparent " + C_PRIMARY + " transparent;"
                + "-fx-border-width:0 0 2 0;-fx-background-radius:0;-fx-padding:10 0 10 0;";
        tf.setStyle(base);
        tf.focusedProperty().addListener((obs, o, n) -> tf.setStyle(n ? focused : base));
        field.getChildren().addAll(lbl, tf);
        return field;
    }

    private void playSound() {
        try {
            String soundPath = getClass().getResource("/Perfect.wav").toExternalForm();
            AudioClip clip = new AudioClip(soundPath);
            clip.play();
        } catch (Exception e) {
            System.err.println("Could not find sound file");
        }
    }

    private HBox userRow(String initials, String name, String role, boolean active) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 24, 10, 24));
        row.setCursor(active ? Cursor.DEFAULT : Cursor.HAND);

        String baseStyle = active
                ? bg(C_SURFACE_HIGH) + fg(C_PRIMARY)
                + "-fx-border-color:transparent transparent transparent " + C_PRIMARY + ";-fx-border-width:0 0 0 3;"
                : "-fx-background-color:transparent;" + fg(C_ON_SURFACE_VAR);
        row.setStyle(baseStyle);
        if (!active) {
            row.setOnMouseEntered(e -> row.setStyle(bg(C_SURFACE_HIGHEST) + fg(C_ON_SURFACE_VAR)));
            row.setOnMouseExited(e  -> row.setStyle(baseStyle));
        }

        StackPane avatar = new StackPane();
        avatar.setMinSize(32, 32); avatar.setMaxSize(32, 32);
        avatar.setStyle("-fx-background-radius:16;" + bg(active ? C_PRIMARY_CONTAINER : C_SURFACE_HIGHEST));
        Label initLbl = new Label(initials);
        initLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;" + fg(active ? C_PRIMARY : C_OUTLINE));
        avatar.getChildren().add(initLbl);

        VBox info = new VBox(2);
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-size:13px;-fx-font-weight:600;" + (active ? fg(C_PRIMARY) : fg(C_ON_SURFACE)));
        info.getChildren().add(nameLbl);
        if (role != null) {
            Label roleLbl = new Label(role.toUpperCase());
            roleLbl.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:9px;-fx-opacity:0.6;");
            info.getChildren().add(roleLbl);
        }
        row.getChildren().addAll(avatar, info);
        return row;
    }

    private HBox sidebarAction(String icon, String label, boolean isError) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 16, 8, 16));
        row.setCursor(Cursor.HAND);
        String color   = isError ? C_ERROR : C_ON_SURFACE_VAR;
        String hoverBg = isError ? C_ERROR_CONTAINER + "33" : C_SURFACE_HIGHEST;
        row.setStyle("-fx-background-color:transparent;-fx-background-radius:8;");
        row.setOnMouseEntered(e -> row.setStyle(bg(hoverBg) + "-fx-background-radius:8;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-background-color:transparent;-fx-background-radius:8;"));
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:15px;" + fg(color));
        Label textLbl = new Label(label);
        textLbl.setStyle("-fx-font-size:13px;" + fg(color));
        row.getChildren().addAll(iconLbl, textLbl);
        return row;
    }
    private void resetSession() {
        myUserId = "User-" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        myCrdt = new DocumentCRDT(myUserId);
        connectedUsers.clear();
        editor = null;
        boldActive = false;
        italicActive = false;
        myRole = "editor";
        historyReplayDone = false;
    }


    private String myUserId = "User-" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    private String myRole = "editor";
    private DocumentCRDT myCrdt = new DocumentCRDT(myUserId);
    private boolean historyReplayDone = false;
    private CollabClient webSocketClient;

    private InlineCssTextArea editor;

    private Button  boldBtn;
    private Button  italicBtn;
    private boolean boldActive   = false;
    private boolean italicActive = false;

    public static void main(String[] args) { launch(args); }

    public static class RemoteUser {
        String id;
        String name;
        public RemoteUser(String id, String name) { this.id = id; this.name = name; }
    }

    private java.util.Map<String, RemoteUser> connectedUsers = new java.util.HashMap<>();
    private VBox userListContainer;

    private void refreshUserList() {
        Platform.runLater(() -> {
            if (userListContainer == null) return;

            userListContainer.getChildren().clear();

            String initials = myUserId.length() > 2 ? myUserId.substring(0, 2) : myUserId;
            userListContainer.getChildren().add(userRow(initials, myUserId + " (You)", "Lead Editor", true));

            for (RemoteUser u : connectedUsers.values()) {
                String uInitials = u.name.length() > 2 ? u.name.substring(0, 2) : u.name;
                userListContainer.getChildren().add(userRow(uInitials, u.name, "Editor", false));
            }
        });
    }

}