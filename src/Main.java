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

/**
 * TextDoc - Two-screen JavaFX app
 *  Screen 1: Welcome / Connect
 *  Screen 2: Collaborative Editor  ← opens when "Start New Project" is clicked
 *
 * VM options:
 *   --module-path "G:\openjfx-26_windows-x64_bin-sdk\javafx-sdk-26\lib" --add-modules javafx.controls,javafx.graphics,javafx.base
 */
public class Main extends Application {

    // Colour palette
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

    private Stage primaryStage;

    private static String bg(String h) { return "-fx-background-color:" + h + ";"; }
    private static String fg(String h) { return "-fx-text-fill:" + h + ";"; }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("TextDoc");
        stage.setWidth(1150);
        stage.setHeight(740);
        showWelcome();
        stage.show();
    }

    // HANDLE REMOTE OPERATION
    // Takes JSON from the WebSocket and updates the UI
    public void handleRemoteOperation(String jsonMessage) {

        // CRITICAL: WebSockets run in the background.
        // Platform.runLater forces this code to run on the main UI thread so JavaFX doesn't crash!
        Platform.runLater(() -> {
            try {
                Gson gson = new Gson();
                // Turn the raw string into a flexible JSON Object
                JsonObject op = gson.fromJson(jsonMessage, JsonObject.class);

                String type = op.get("type").getAsString();
                String siteId = op.get("siteId").getAsString();
                int clock = op.get("clock").getAsInt();

                if (type.equals("INSERT")) {
                    char value = op.get("value").getAsString().charAt(0);

                    // Safely handle the afterSiteId (since the first character is null)
                    String afterSiteId = null;
                    if (op.has("afterSiteId") && !op.get("afterSiteId").isJsonNull()) {
                        afterSiteId = op.get("afterSiteId").getAsString();
                    }
                    int afterClock = op.get("afterClock").getAsInt();

                    // Rebuild the Node and feed it to the CRDT
                    CharacterNode incomingNode = new CharacterNode(siteId, clock, value, afterSiteId, afterClock);
                    myCrdt.remoteInsert(incomingNode);

                } else if (type.equals("DELETE")) {
                    myCrdt.remoteDelete(siteId, clock);
                }

                // Force the screen to update and match the new CRDT state
                int cursorIndex = editor.getCaretPosition();
                editor.setText(myCrdt.getVisibleText());
                editor.positionCaret(cursorIndex);

            } catch (Exception e) {
                System.err.println("Failed to parse incoming JSON: " + jsonMessage);
                e.printStackTrace();
            }
        });
    }
    // =========================================================================
    //  SCREEN 1 — WELCOME
    // =========================================================================
    private void showWelcome() {
        BorderPane root = new BorderPane();
        root.setStyle(bg(C_BG));
        root.setTop(buildWelcomeHeader());
        root.setCenter(buildWelcomeMain());
        root.setBottom(buildStatusBar());
        primaryStage.setScene(new Scene(root));
    }

    // Welcome header
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

    // Welcome main (left + right columns)
    private HBox buildWelcomeMain() {
        HBox main = new HBox();
        VBox left  = buildConnectPanel();
        HBox.setHgrow(left, Priority.ALWAYS);
        main.setAlignment(Pos.CENTER); // Centers the remaining panel
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

        // 1. Create the form fields using your helper
        VBox serverAddressBox = formField("Server Address", "ws://your-server:port", "ws://localhost:8888");
        VBox roomCodeBox = formField("Room ID", "Enter session code...", "");

        // 2. Extract the actual TextField components from those VBoxes so we can read them
        TextField serverAddressInput = (TextField) serverAddressBox.getChildren().get(1);
        TextField roomCodeInput = (TextField) roomCodeBox.getChildren().get(1);

        // 3. Build the form
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
                String targetServer = serverAddressInput.getText();
                System.out.println("Attempting to connect to: " + targetServer);


                webSocketClient = new CollabClient(targetServer, message -> handleRemoteOperation(message));
                webSocketClient.connect();

                showEditor();

            } catch (Exception ex) {
                System.err.println("Failed to build the client!");
                ex.printStackTrace();
            }
        });
        return panel;


    }



    // Shared status bar
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

        Label offline = new Label("OFFLINE");
        offline.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:9px;-fx-font-weight:bold;");
        Rectangle divLine = new Rectangle(1, 12, Color.web(C_OUTLINE_VAR));
        divLine.setOpacity(0.3);
        Label version = new Label("v1.0.4-stable");
        version.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:9px;");

        HBox left = new HBox(8, dot, offline, divLine, version);
        left.setAlignment(Pos.CENTER_LEFT);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Label sync     = new Label("☁ Sync Enabled");
        sync.setStyle(fg(C_TERTIARY) + "-fx-font-size:9px;-fx-font-weight:600;");
        Label encoding = new Label("UTF-8");
        encoding.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:9px;");

        HBox right = new HBox(16, sync, encoding);
        right.setAlignment(Pos.CENTER_RIGHT);

        bar.getChildren().addAll(left, sp, right);
        return bar;
    }

    // =========================================================================
    //  SCREEN 2 — COLLABORATIVE EDITOR
    // =========================================================================
    private void showEditor() {
        BorderPane root = new BorderPane();
        root.setStyle(bg(C_BG));
        root.setTop(buildEditorTop());
        root.setCenter(buildEditorCenter());
        root.setBottom(buildEditorFooter());
        primaryStage.setScene(new Scene(root));
    }

    // Editor top (header + toolbar)
    private VBox buildEditorTop() {
        // header
        HBox header = new HBox();
        header.setStyle(bg(C_BG));
        header.setPadding(new Insets(10, 24, 10, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        Label logo = new Label("TextDoc");
        logo.setStyle(fg(C_ON_SURFACE) + "-fx-font-size:18px;-fx-font-weight:bold;");

        Rectangle divLine = new Rectangle(1, 16, Color.web(C_OUTLINE_VAR));
        divLine.setOpacity(0.3);

        Label tag = new Label("✏  COLLABORATIVE EDITOR");
        tag.setStyle(fg(C_PRIMARY) + "-fx-font-size:10px;-fx-font-weight:bold;");

        HBox leftGroup = new HBox(10, logo, divLine, tag);
        leftGroup.setAlignment(Pos.CENTER_LEFT);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        // search box
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

        HBox rightIcons = new HBox(4, iconBtn("⚙"), iconBtn("👤"));
        rightIcons.setAlignment(Pos.CENTER);
        rightIcons.setPadding(new Insets(0, 0, 0, 8));

        header.getChildren().addAll(leftGroup, sp);

        // toolbar
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

        HBox fmtBar = new HBox(2);
        fmtBar.setAlignment(Pos.CENTER);
        fmtBar.setStyle(bg(C_SURFACE_HIGH) + "-fx-background-radius:20;-fx-padding:4;");
        fmtBar.getChildren().addAll(fmtBtn("B"), fmtBtn("I"),
                new Separator(Orientation.VERTICAL), fmtBtn("≡"), fmtBtn("🔗"));

        toolbar.getChildren().addAll(statusGrp, tsp);

        return new VBox(header, toolbar);
    }

    // ── Editor center (textarea + sidebar) ────────────────────────────────────
    private HBox buildEditorCenter() {
        HBox center = new HBox();

        // main text area
        VBox editorArea = new VBox(16);
        editorArea.setPadding(new Insets(56, 80, 56, 80));
        editorArea.setStyle(bg(C_BG));
        HBox.setHgrow(editorArea, Priority.ALWAYS);

        Label docTitle = new Label("Draft_01");
        docTitle.setStyle(fg(C_ON_SURFACE) + "-fx-font-size:38px;-fx-font-weight:900;-fx-opacity:0.10;");


        editor = new TextArea("");
        editor.setFont(Font.font("Monospaced", 14));
        editor.setStyle(bg(C_BG) + fg(C_ON_SURFACE)
                + "-fx-border-color:transparent;-fx-background-radius:0;-fx-padding:0;");
        editor.setWrapText(true);
        VBox.setVgrow(editor, Priority.ALWAYS);

        // =========================================================
        //  CRDT INTEGRATION
        // =========================================================

        // 1. INTERCEPT TYPING
        editor.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            String characterStr = event.getCharacter();
            if (characterStr.isEmpty() || characterStr.charAt(0) < 32) return;

            int cursorIndex = editor.getCaretPosition();
            String afterSiteId = null;
            int afterClock = 0;

            if (cursorIndex > 0) {
                CharacterNode prevNode = myCrdt.getVisibleNodeAt(cursorIndex - 1);
                if (prevNode != null) {
                    afterSiteId = prevNode.siteId;
                    afterClock = prevNode.clock;
                }
            }

            char letter = characterStr.charAt(0);

            // 1. Update local CRDT and get the new Node
            CharacterNode newNode = myCrdt.localInsert(letter, afterSiteId, afterClock);

            // 2. Format the operation as a JSON String
            String afterIdJson = (newNode.afterSiteId == null) ? "null" : "\"" + newNode.afterSiteId + "\"";
            String insertMessage = String.format(
                    "{\"type\": \"INSERT\", \"siteId\": \"%s\", \"clock\": %d, \"value\": \"%s\", \"afterSiteId\": %s, \"afterClock\": %d}",
                    newNode.siteId, newNode.clock, newNode.value, afterIdJson, newNode.afterClock
            );

            // 3. Send it to the server!
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.send(insertMessage);
            }

            // Redraw UI
            event.consume();
            editor.setText(myCrdt.getVisibleText());
            editor.positionCaret(cursorIndex + 1);
        });

        // 2. INTERCEPT DELETING
        editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.BACK_SPACE) {
                int cursorIndex = editor.getCaretPosition();

                if (cursorIndex > 0) {
                    CharacterNode nodeToDelete = myCrdt.getVisibleNodeAt(cursorIndex - 1);

                    if (nodeToDelete != null) {
                        // 1. Update local CRDT
                        myCrdt.localDelete(nodeToDelete.siteId, nodeToDelete.clock);

                        // 2. Format the DELETE operation as JSON
                        String deleteMessage = String.format(
                                "{\"type\": \"DELETE\", \"siteId\": \"%s\", \"clock\": %d}",
                                nodeToDelete.siteId, nodeToDelete.clock
                        );

                        // 3. Send it to the server!
                        if (webSocketClient != null && webSocketClient.isOpen()) {
                            webSocketClient.send(deleteMessage);
                        }
                    }

                    event.consume();
                    editor.setText(myCrdt.getVisibleText());
                    editor.positionCaret(cursorIndex - 1);
                }
            }
        });
        // =========================================================
        //  END OF CRDT INTEGRATION
        // =========================================================

        editorArea.getChildren().addAll(docTitle, editor);

        // sidebar
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


        userList.getChildren().addAll(
                userRow(myUserId.substring(5), myUserId + " (You)", "Lead Editor", true)
        );

        // bottom of sidebar
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

        // back-to-welcome option in Help row
        HBox helpRow   = sidebarAction("❓", "Help",   false);
        HBox logoutRow = sidebarAction("🚪", "Logout", true);
        logoutRow.setOnMouseClicked(e -> showWelcome());

        bottomActions.getChildren().add(logoutRow);
        sidebar.getChildren().addAll(sideHdr, userList, bottomActions);

        center.getChildren().addAll(editorArea, sidebar);
        return center;
    }

    // Editor footer
    private HBox buildEditorFooter() {
        HBox bar = new HBox();
        bar.setStyle(bg(C_SURFACE_HIGH));
        bar.setPadding(new Insets(6, 24, 6, 24));
        bar.setAlignment(Pos.CENTER_LEFT);

        Label statusHdr = new Label("STATUS");
        statusHdr.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:8px;-fx-font-weight:bold;-fx-opacity:0.4;");
        Label idleLbl = new Label("Idle");
        idleLbl.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:11px;");

        Label syncHdr = new Label("SYNC");
        syncHdr.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:8px;-fx-font-weight:bold;-fx-opacity:0.4;");
        Label cloudIcon = new Label("☁");
        cloudIcon.setStyle(fg(C_PRIMARY) + "-fx-font-size:13px;");

        HBox leftGroup = new HBox(16,
                new HBox(6, statusHdr, idleLbl),
                new HBox(4, syncHdr, cloudIcon));
        leftGroup.setAlignment(Pos.CENTER_LEFT);
        ((HBox) leftGroup.getChildren().get(0)).setAlignment(Pos.CENTER_LEFT);
        ((HBox) leftGroup.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        HBox rightGroup = new HBox(16);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);
        for (String t : new String[]{"UTF-8", "LF", "Master"}) {
            Label l = new Label(t);
            l.setStyle(fg(C_ON_SURFACE_VAR) + "-fx-font-size:11px;-fx-opacity:0.6;");
            rightGroup.getChildren().add(l);
        }

        bar.getChildren().addAll(leftGroup, sp, rightGroup);
        return bar;
    }

    // =========================================================================
    //  SHARED COMPONENT BUILDERS
    // =========================================================================
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
    private String myUserId = "User-" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    private DocumentCRDT myCrdt = new DocumentCRDT(myUserId);
    private CollabClient webSocketClient;
    private TextArea editor;
    public static void main(String[] args) { launch(args); }
}
