import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

// ECommerceApp.java
public class ECommerceApp {

    // Models (User, Product, Order, etc.)
    static class User implements Serializable {
        enum Role { ADMIN, CUSTOMER }
        private static final long serialVersionUID = 1L;
        private final UUID id;
        private String name;
        private String email;
        private String passwordHash;
        private Role role;

        public User(String name, String email, String password, Role role) {
            this.id = UUID.randomUUID();
            this.name = name;
            this.email = email.toLowerCase();
            this.passwordHash = hash(password);
            this.role = role;
        }

        public UUID getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public Role getRole() { return role; }
        public void setName(String n) { name = n; }
        public void setRole(Role r) { role = r; }

        public boolean checkPassword(String pw) {
            return hash(pw).equals(passwordHash);
        }

        public void setPassword(String pw) { passwordHash = hash(pw); }

        private String hash(String s) {
            return Integer.toString(Objects.hash(s));
        }

        @Override
        public String toString() {
            return name + " <" + email + "> (" + role + ")";
        }
    }

    static class Product implements Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID id;
        private String name;
        private String description;
        private double price;
        private int stock;
        private String category;

        public Product(String name, String desc, double price, int stock, String category) {
            this.id = UUID.randomUUID();
            this.name = name;
            this.description = desc;
            this.price = price;
            this.stock = stock;
            this.category = category;
        }

        public UUID getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public double getPrice() { return price; }
        public int getStock() { return stock; }
        public String getCategory() { return category; }

        public void setName(String n) { name = n; }
        public void setDescription(String d) { description = d; }
        public void setPrice(double p) { price = p; }
        public void setStock(int s) { stock = s; }
        public void setCategory(String c) { category = c; }
    }

    static class Order implements Serializable {
        private static final long serialVersionUID = 1L;
        enum Status { PENDING, SHIPPED, DELIVERED, CANCELLED }
        private final UUID id;
        private final UUID userId;
        private final LocalDateTime createdAt;
        private final List<OrderItem> items;
        private Status status;
        private final double total;

        public Order(UUID userId, List<OrderItem> items) {
            this.id = UUID.randomUUID();
            this.userId = userId;
            this.createdAt = LocalDateTime.now();
            this.items = new ArrayList<>(items);
            this.total = items.stream().mapToDouble(i -> i.price * i.quantity).sum();
            this.status = Status.PENDING;
        }

        public UUID getId() { return id; }
        public UUID getUserId() { return userId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public List<OrderItem> getItems() { return items; }
        public Status getStatus() { return status; }
        public void setStatus(Status s) { status = s; }
        public double getTotal() { return total; }
    }

    static class OrderItem implements Serializable {
        private static final long serialVersionUID = 1L;
        final UUID productId;
        final String productName;
        final int quantity;
        final double price;

        public OrderItem(UUID productId, String productName, int qty, double price) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = qty;
            this.price = price;
        }
    }

    // -------------------------------
    // Repository / DataStore (persistence)
    // -------------------------------

    static class Repository implements Serializable {
        private static final long serialVersionUID = 1L;
        List<User> users = new ArrayList<>();
        List<Product> products = new ArrayList<>();
        List<Order> orders = new ArrayList<>();
    }

    static class DataStore {
        private static final String DATA_FILE = "data.ser";
        private static DataStore instance;
        Repository repo;

        private DataStore() {
            repo = load();
            if (repo == null) {
                repo = new Repository();
                seedDefaultData(repo);
                save();
            }
        }

        public static synchronized DataStore getInstance() {
            if (instance == null) instance = new DataStore();
            return instance;
        }

        private Repository load() {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
                return (Repository) ois.readObject();
            } catch (Exception e) {
                return null;
            }
        }

        public synchronized void save() {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
                oos.writeObject(repo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void seedDefaultData(Repository r) {
            User admin = new User("Admin", "admin@shop.com", "admin123", User.Role.ADMIN);
            User c1 = new User("Ali", "ali@example.com", "pass", User.Role.CUSTOMER);
            r.users.add(admin);
            r.users.add(c1);

            r.products.add(new Product("Chocolate Cake", "Delicious dark chocolate cake (8 inch)", 25.0, 15, "Cakes"));
            r.products.add(new Product("Vanilla Cupcakes (6)", "Soft vanilla cupcakes (pack of 6)", 10.0, 40, "Cupcakes"));
            r.products.add(new Product("Strawberry Tart", "Fresh strawberry tart", 18.0, 10, "Tarts"));
            r.products.add(new Product("Red Velvet Slice", "Single slice of red velvet cake", 6.0, 30, "Slices"));
            r.products.add(new Product("Lemon Cheesecake", "Creamy lemon cheesecake", 22.0, 8, "Cakes"));
            r.products.add(new Product("Tiramisu", "Classic Italian coffee-flavored dessert", 28.0, 12, "Cakes"));
            r.products.add(new Product("Macarons (12)", "Assorted French macarons", 15.0, 25, "Pastries"));
            r.products.add(new Product("Carrot Cake", "Moist carrot cake with cream cheese frosting", 20.0, 18, "Cakes"));
        }
    }

    // Controllers
    static class AuthController {
        private final DataStore ds = DataStore.getInstance();

        public User login(String email, String password) {
            for (User u : ds.repo.users) {
                if (u.getEmail().equalsIgnoreCase(email) && u.checkPassword(password)) return u;
            }
            return null;
        }

        public User register(String name, String email, String password, User.Role role) throws IllegalArgumentException {
            for (User u : ds.repo.users) {
                if (u.getEmail().equalsIgnoreCase(email)) throw new IllegalArgumentException("Email already registered.");
            }
            User user = new User(name, email, password, role);
            ds.repo.users.add(user);
            ds.save();
            return user;
        }
    }

    static class ProductController {
        private final DataStore ds = DataStore.getInstance();

        public List<Product> listAll() {
            return new ArrayList<>(ds.repo.products);
        }

        public Product addProduct(String name, String desc, double price, int stock, String category) {
            Product p = new Product(name, desc, price, stock, category);
            ds.repo.products.add(p);
            ds.save();
            return p;
        }

        public void updateProduct(Product p) {
            ds.save();
        }

        public void removeProduct(UUID id) {
            ds.repo.products.removeIf(p -> p.getId().equals(id));
            ds.save();
        }

        public Optional<Product> findById(UUID id) {
            return ds.repo.products.stream().filter(p -> p.getId().equals(id)).findFirst();
        }

        public List<Product> search(String q, String category) {
            String qq = q == null ? "" : q.toLowerCase();
            List<Product> out = new ArrayList<>();
            for (Product p : ds.repo.products) {
                boolean matches = (p.getName().toLowerCase().contains(qq) ||
                        p.getDescription().toLowerCase().contains(qq));
                boolean catOK = (category == null || category.isEmpty() || p.getCategory().equalsIgnoreCase(category));
                if (matches && catOK) out.add(p);
            }
            return out;
        }
    }

    static class OrderController {
        private final DataStore ds = DataStore.getInstance();

        public Order placeOrder(User user, List<OrderItem> items) throws IllegalArgumentException {
            for (OrderItem it : items) {
                Product p = ds.repo.products.stream().filter(px -> px.getId().equals(it.productId)).findFirst().orElse(null);
                if (p == null) throw new IllegalArgumentException("Product not found: " + it.productName);
                if (p.getStock() < it.quantity) throw new IllegalArgumentException("Insufficient stock for: " + p.getName());
            }
            for (OrderItem it : items) {
                Product p = ds.repo.products.stream().filter(px -> px.getId().equals(it.productId)).findFirst().orElse(null);
                p.setStock(p.getStock() - it.quantity);
            }
            Order order = new Order(user.getId(), items);
            ds.repo.orders.add(order);
            ds.save();
            return order;
        }

        public List<Order> getOrdersForUser(UUID userId) {
            List<Order> out = new ArrayList<>();
            for (Order o : ds.repo.orders) if (o.getUserId().equals(userId)) out.add(o);
            return out;
        }

        public List<Order> getAllOrders() {
            return new ArrayList<>(ds.repo.orders);
        }

        public void updateOrderStatus(UUID orderId, Order.Status status) {
            for (Order o : ds.repo.orders) if (o.getId().equals(orderId)) o.setStatus(status);
            ds.save();
        }
    }

    // UI / Views (Swing)

    private final AuthController authController = new AuthController();
    private final ProductController productController = new ProductController();
    private final OrderController orderController = new OrderController();

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private User currentUser;
    private final Cart cart = new Cart();

    // Modern light palette
    private static final Color BG = new Color(248, 249, 250); // very light gray
    private static final Color PANEL = new Color(255, 255, 255); // white for cards
    private static final Color ACCENT = new Color(83, 116, 171); // soft blue accent
    private static final Color ACCENT_DARK = new Color(60, 85, 133);
    private static final Color BUTTON = new Color(53, 73, 255);
    private static final Color BUTTON_HOVER = new Color(216, 228, 244);
    private static final Color TEXT = new Color(255, 255, 255); // dark slate
    private static final Color TEXT_LIGHT = new Color(106, 116, 130);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER = new Color(230, 233, 237);

    private static final GradientPaint HEADER_GRADIENT = new GradientPaint(0, 0, new Color(255,255,255), 0, 120, new Color(245,247,250));

    // Decimal format for prices
    private static final DecimalFormat MONEY = new DecimalFormat("0.00");

    // Fonts
    private Font fontTitle, fontSubtitle, fontBody, fontSmall;

    // entry point
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("ProgressBar.arc", 12);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ECommerceApp app = new ECommerceApp();
            app.init();
        });
    }

    void init() {
        loadFonts();

        frame = new JFrame("Sweet Slice — Premium Cake Shop");
        frame.setSize(1200, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        try {
            frame.setIconImages(createAppIcon());
        } catch (Exception e) {
            // ignore icon failure
        }

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(BG);

        // add views
        mainPanel.add(new AuthView(this), "AUTH");
        mainPanel.add(new StoreView(this), "STORE");
        mainPanel.add(new AdminView(this), "ADMIN");
        mainPanel.add(new OrdersView(this), "ORDERS");

        frame.add(mainPanel);
        frame.setVisible(true);

        showAuth();
    }

    private void loadFonts() {
        try {
            fontTitle = new Font("Segoe UI", Font.BOLD, 28);
            fontSubtitle = new Font("Segoe UI", Font.BOLD, 18);
            fontBody = new Font("Segoe UI", Font.PLAIN, 14);
            fontSmall = new Font("Segoe UI", Font.PLAIN, 12);
        } catch (Exception e) {
            fontTitle = new Font("SansSerif", Font.BOLD, 28);
            fontSubtitle = new Font("SansSerif", Font.BOLD, 18);
            fontBody = new Font("SansSerif", Font.PLAIN, 14);
            fontSmall = new Font("SansSerif", Font.PLAIN, 12);
        }
    }

    @SuppressWarnings("unchecked")
    private List<? extends Image> createAppIcon() {
        int size = 64;
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // background circle
        g2d.setColor(new Color(235, 241, 247));
        g2d.fillOval(0, 0, size, size);

        // cake base
        g2d.setColor(ACCENT);
        g2d.fillRoundRect(size/6, size/2, size*2/3, size/8, 8, 8);

        // cake top
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillRoundRect(size/6, size/3, size*2/3, size/4, 10, 10);

        // candle
        g2d.setColor(ACCENT_DARK);
        g2d.fillRect(size/2 - 2, size/6, 4, size/6);

        g2d.dispose();
        return Collections.singletonList((Image) icon);
    }

    // navigation helpers
    public void showAuth() { cardLayout.show(mainPanel, "AUTH"); }
    public void showStore() { refreshStore(); cardLayout.show(mainPanel, "STORE"); }
    public void showAdmin() { refreshAdmin(); cardLayout.show(mainPanel, "ADMIN"); }
    public void showOrders() { refreshOrders(); cardLayout.show(mainPanel, "ORDERS"); }

    // login/register actions called by AuthView
    public void handleLogin(String email, String password) {
        User u = authController.login(email, password);
        if (u == null) {
            JOptionPane.showMessageDialog(frame, "Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        currentUser = u;
        cart.clear(); // new session cart
        if (u.getRole() == User.Role.ADMIN) showAdmin();
        else showStore();
    }

    public void handleRegister(String name, String email, String password, User.Role role) {
        try {
            User u = authController.register(name, email, password, role);
            JOptionPane.showMessageDialog(frame, "Registered! Please login.", "OK", JOptionPane.INFORMATION_MESSAGE);
            showAuth();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void logout() {
        currentUser = null;
        cart.clear();
        showAuth();
    }

    public User getCurrentUser() { return currentUser; }
    public Cart getCart() { return cart; }
    public ProductController getProductController() { return productController; }
    public OrderController getOrderController() { return orderController; }

    // refreshers to update view data
    void refreshStore() {
        Component c = findByName(mainPanel, "STORE");
        if (c instanceof StoreView) ((StoreView) c).refresh();
    }
    void refreshAdmin() {
        Component c = findByName(mainPanel, "ADMIN");
        if (c instanceof AdminView) ((AdminView) c).refresh();
    }
    void refreshOrders() {
        Component c = findByName(mainPanel, "ORDERS");
        if (c instanceof OrdersView) ((OrdersView) c).refresh();
    }

    private Component findByName(Container parent, String name) {
        for (Component comp : parent.getComponents()) {
            if (name.equals(comp.getName())) return comp;
        }
        return null;
    }

    // ------------------------
    // Views Implementation
    // ------------------------

    // AuthView: login/register
    class AuthView extends JPanel {
        private final JTextField emailField = new JTextField(20);
        private final JPasswordField passwordField = new JPasswordField(20);
        private final JTextField regName = new JTextField(15);
        private final JTextField regEmail = new JTextField(15);
        private final JPasswordField regPass = new JPasswordField(15);


        AuthView(ECommerceApp app) {
            setName("AUTH");
            setLayout(new GridBagLayout());
            setBackground(BG);

            styleTextField(emailField);
            styleTextField(passwordField);
            styleTextField(regName);
            styleTextField(regEmail);
            styleTextField(regPass);
            styleTextFieldWithStroke(emailField);
            styleTextFieldWithStroke(regName);
            styleTextFieldWithStroke(regEmail);
            stylePasswordFieldWithStroke(passwordField);
            stylePasswordFieldWithStroke(regPass);


            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(15, 15, 15, 15);
            gbc.fill = GridBagConstraints.BOTH;

            // left: branding with clean design
            JPanel brand = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setPaint(HEADER_GRADIENT);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            brand.setBackground(BG);
            brand.setBorder(new EmptyBorder(30, 30, 30, 30));
            JLabel title = new JLabel("<html><div style='text-align:center;'><span style='font-size:32pt;font-weight:700;color:#445d86'>Sweet Slice</span><br><span style='font-size:12pt;color:#6b6f78'>Artisan Bakery & Cake Shop</span></div></html>");
            title.setFont(fontTitle);
            brand.add(title, BorderLayout.CENTER);

            JLabel hero = new JLabel("<html><div style='padding:20px 10px;color:#4b5158;text-align:center;font-size:11pt'>Premium hand-crafted cakes — order online and enjoy fresh delivery.</div></html>");
            hero.setFont(fontBody);
            brand.add(hero, BorderLayout.SOUTH);

            gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5; gbc.weighty = 1.0;
            add(brand, gbc);

            // right: forms with card design
            JPanel forms = new JPanel(new GridBagLayout());
            forms.setBackground(PANEL);
            forms.setBorder(new CompoundBorder(
                    new LineBorder(BORDER, 1),
                    new EmptyBorder(25, 25, 25, 25)
            ));

            GridBagConstraints f = new GridBagConstraints();
            f.insets = new Insets(10, 10, 10, 10);
            f.gridx = 0; f.gridy = 0; f.anchor = GridBagConstraints.WEST;
            f.gridwidth = 2;

            JLabel loginTitle = new JLabel("Login to Your Account");
            loginTitle.setFont(fontSubtitle);
            loginTitle.setForeground(ACCENT_DARK);
            forms.add(loginTitle, f);

            f.gridwidth = 1;
            f.gridy++; forms.add(new JLabel("Email:"), f);
            f.gridx = 1; forms.add(emailField, f);
            f.gridx = 0; f.gridy++; forms.add(new JLabel("Password:"), f);
            f.gridx = 1; forms.add(passwordField, f);
            f.gridx = 1; f.gridy++;
            JButton loginBtn = styledButton("Login", ACCENT, ACCENT_DARK);
            loginBtn.setPreferredSize(new Dimension(120, 36));
            forms.add(loginBtn, f);

            loginBtn.addActionListener(e -> app.handleLogin(emailField.getText(), new String(passwordField.getPassword())));

            // register panel (below)
            f.gridx = 0; f.gridy++; f.gridwidth = 2;
            forms.add(createSeparator(), f);

            f.gridwidth = 1; f.gridy++;
            JLabel regTitle = new JLabel("Create New Account");
            regTitle.setFont(fontSubtitle);
            regTitle.setForeground(ACCENT_DARK);
            forms.add(regTitle, f);

            f.gridy++; forms.add(new JLabel("Name:"), f);
            f.gridx = 1; forms.add(regName, f);
            f.gridx = 0; f.gridy++; forms.add(new JLabel("Email:"), f);
            f.gridx = 1; forms.add(regEmail, f);
            f.gridx = 0; f.gridy++; forms.add(new JLabel("Password:"), f);
            f.gridx = 1; forms.add(regPass, f);
            f.gridx = 1; f.gridy++;
            JButton regBtn = styledButton("Register", BUTTON, BUTTON_HOVER);
            regBtn.setPreferredSize(new Dimension(160, 36));
            forms.add(regBtn, f);

            regBtn.addActionListener(e -> app.handleRegister(regName.getText(), regEmail.getText(), new String(regPass.getPassword()), User.Role.CUSTOMER));

            gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.5;
            add(forms, gbc);

            JLabel info = new JLabel("<html><div style='text-align:center;padding:10px;'><i>Default admin: <b>admin@shop.com</b> / <b>admin123</b></i></div></html>");
            info.setFont(fontSmall);
            gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
            add(info, gbc);
        }

        private JSeparator createSeparator() {
            JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
            sep.setForeground(BORDER);
            sep.setPreferredSize(new Dimension(200, 1));
            return sep;
        }

        private void styleTextField(JTextField field) {
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            field.setBackground(Color.WHITE);
            field.setFont(fontBody);
        }
        private void styleTextFieldWithStroke(JTextField field) {
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 0, 0, 58), 1), // subtle gray stroke
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            field.setBackground(Color.WHITE);
            field.setFont(fontBody);
        }

        private void stylePasswordFieldWithStroke(JPasswordField field) {
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 0, 0, 58), 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            field.setBackground(Color.WHITE);
            field.setFont(fontBody);
        }

    }

    // StoreView: product browsing (card grid), cart, checkout
    class StoreView extends JPanel {
        private final JPanel cardsPanel = new JPanel();
        private final JScrollPane scrollPane;
        private final JTextField searchField = new JTextField(16);
        private final JComboBox<String> categoryBox = new JComboBox<>();
        private final JLabel cartLabel = new JLabel();
        private final JLabel welcomeLabel = new JLabel();


        StoreView(ECommerceApp app) {
            setName("STORE");
            setLayout(new BorderLayout(12,12));
            setBackground(BG);
            setBorder(new EmptyBorder(15,15,15,15));

            // Header: title + actions
            JPanel header = new JPanel(new BorderLayout(10,10));
            header.setBackground(BG);
            JLabel title = new JLabel("<html><span style='font-size:18pt;color:#2e3b4f'><b>Sweet Slice</b></span><br><span style='font-size:10pt;color:#6b6f78'>Freshly baked — delivered to your door</span></html>");
            title.setBorder(new EmptyBorder(6,6,6,6));
            header.add(title, BorderLayout.WEST);

            // center search area
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            searchPanel.setBackground(BG);
            searchPanel.setBorder(new CompoundBorder(
                    new LineBorder(BORDER, 1),
                    new EmptyBorder(8, 10, 8, 10)
            ));

            styleTextField(searchField);
            searchField.setPreferredSize(new Dimension(180, 32));
            searchPanel.add(new JLabel("Search:"));
            searchPanel.add(searchField);

            searchPanel.add(new JLabel("Category:"));
            categoryBox.addItem("");
            Set<String> cats = new TreeSet<>();
            for (Product p : productController.listAll()) cats.add(p.getCategory());
            for (String c : cats) categoryBox.addItem(c);
            styleComboBox(categoryBox);
            searchPanel.add(categoryBox);

            JButton searchBtn = styledButton("Search", BUTTON, BUTTON_HOVER);
            searchBtn.setPreferredSize(new Dimension(100, 34));
            searchPanel.add(searchBtn);
            header.add(searchPanel, BorderLayout.CENTER);

            // right actions
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
            actions.setBackground(BG);
            updateCartLabel();
            cartLabel.setFont(fontBody);
            actions.add(cartLabel);
            JButton myOrders = styledButton("My Orders", BUTTON, BUTTON_HOVER);
            JButton adminBtn = styledButton("Admin", BUTTON, BUTTON_HOVER);
            JButton logout = styledButton("Logout", BUTTON, BUTTON_HOVER);
            actions.add(myOrders);
            actions.add(adminBtn);
            actions.add(logout);
            header.add(actions, BorderLayout.EAST);

            add(header, BorderLayout.NORTH);

            // Cards panel in center
            cardsPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 20, 20));
            cardsPanel.setBackground(BG);
            scrollPane = new JScrollPane(cardsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            add(scrollPane, BorderLayout.CENTER);

            // Bottom bar
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottom.setBackground(BG);
            bottom.setBorder(new EmptyBorder(10, 0, 0, 0));
            JButton viewCart = styledButton("View Cart / Checkout", ACCENT, ACCENT_DARK);
            viewCart.setPreferredSize(new Dimension(200, 40));
            bottom.add(viewCart);
            add(bottom, BorderLayout.SOUTH);

            // Listeners
            searchBtn.addActionListener(e -> doSearch());
            logout.addActionListener(e -> app.logout());
            adminBtn.addActionListener(e -> {
                if (app.getCurrentUser() != null && app.getCurrentUser().getRole() == User.Role.ADMIN) app.showAdmin();
                else JOptionPane.showMessageDialog(frame, "Access denied. Admins only.");
            });
            myOrders.addActionListener(e -> app.showOrders());

            viewCart.addActionListener(e -> {
                CartDialog dlg = new CartDialog(frame, cart, productController, orderController, app);
                dlg.setVisible(true);
                updateCartLabel();
                refresh();
            });

            // initial populate
            populateCards(productController.listAll());
        }

        void doSearch() {
            String q = searchField.getText();
            String cat = (String) categoryBox.getSelectedItem();
            List<Product> results = productController.search(q, cat == null ? "" : cat);
            populateCards(results);
        }

        void populateCards(List<Product> products) {
            cardsPanel.removeAll();
            if (products.isEmpty()) {
                JLabel noResults = new JLabel("<html><div style='text-align:center;padding:40px;color:" +
                        String.format("#%02x%02x%02x", TEXT_LIGHT.getRed(), TEXT_LIGHT.getGreen(), TEXT_LIGHT.getBlue()) +
                        ";'>No products found matching your criteria.</div></html>");
                noResults.setFont(fontBody);
                cardsPanel.add(noResults);
            } else {
                for (Product p : products) {
                    cardsPanel.add(productCard(p));
                }
            }
            cardsPanel.revalidate();
            cardsPanel.repaint();
        }

        JPanel productCard(Product p) {
            JPanel card = new JPanel(new BorderLayout());
            card.setPreferredSize(new Dimension(260, 340));
            card.setBackground(CARD_BG);
            card.setBorder(new CompoundBorder(
                    new LineBorder(BORDER, 1),
                    new EmptyBorder(12,12,12,12)
            ));

            // subtle shadow
            card.setBorder(new CompoundBorder(
                    new DropShadowBorder(new Color(200,200,200), 4, 0.25f, 10, false, false, true, true),
                    card.getBorder()
            ));

            // Product "image"
            JPanel imagePanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    GradientPaint gradient = new GradientPaint(0, 0, new Color(248, 249, 250), 0, getHeight(), new Color(242, 244, 247));
                    g2d.setPaint(gradient);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

                    g2d.setColor(ACCENT);
                    int cakeWidth = getWidth()/2;
                    int cakeHeight = getHeight()/3;
                    int x = (getWidth() - cakeWidth)/2;
                    int y = (getHeight() - cakeHeight)/2;

                    g2d.fillRoundRect(x, y + cakeHeight/2, cakeWidth, cakeHeight/2, 10, 10);
                    g2d.setColor(new Color(255,255,255));
                    g2d.fillRoundRect(x, y, cakeWidth, cakeHeight/2, 14, 14);

                    g2d.setColor(ACCENT_DARK);
                    for (int i = 0; i < 4; i++) {
                        g2d.fillOval(x + 12 + i*(cakeWidth/4), y + 10, 8, 8);
                    }
                }
            };
            imagePanel.setPreferredSize(new Dimension(240, 140));
            imagePanel.setBorder(new LineBorder(BORDER, 1));
            card.add(imagePanel, BorderLayout.NORTH);

            // details
            JPanel center = new JPanel(new BorderLayout());
            center.setBackground(CARD_BG);
            center.setBorder(new EmptyBorder(10, 8, 10, 8));
            JLabel name = new JLabel("<html><b>" + p.getName() + "</b></html>");
            name.setFont(fontBody.deriveFont(Font.BOLD));
            name.setForeground(TEXT);
            center.add(name, BorderLayout.NORTH);

            JTextArea desc = new JTextArea(p.getDescription());
            desc.setLineWrap(true);
            desc.setWrapStyleWord(true);
            desc.setEditable(false);
            desc.setOpaque(false);
            desc.setFont(fontSmall);
            desc.setForeground(TEXT_LIGHT);
            desc.setBorder(new EmptyBorder(6, 0, 6, 0));
            center.add(desc, BorderLayout.CENTER);
            card.add(center, BorderLayout.CENTER);

            // bottom
            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setBackground(CARD_BG);
            bottom.setBorder(new EmptyBorder(6, 0, 0, 0));

            JLabel priceLbl = new JLabel("$" + MONEY.format(p.getPrice()));
            priceLbl.setBorder(new EmptyBorder(6,6,6,6));
            priceLbl.setFont(fontSubtitle);
            priceLbl.setForeground(ACCENT_DARK);
            bottom.add(priceLbl, BorderLayout.WEST);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,6));
            right.setBackground(CARD_BG);
            JLabel stock = new JLabel("Stock: " + p.getStock());
            stock.setFont(fontSmall);
            stock.setForeground(p.getStock() > 5 ? TEXT_LIGHT : Color.RED);
            right.add(stock);

            JButton add = styledButton("Add", ACCENT, ACCENT_DARK);
            add.setPreferredSize(new Dimension(84, 32));
            right.add(add);
            bottom.add(right, BorderLayout.EAST);

            add.addActionListener(e -> {
                if (p.getStock() <= 0) {
                    JOptionPane.showMessageDialog(frame, "Sorry, this item is out of stock.", "Out of Stock", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String qtyStr = JOptionPane.showInputDialog(frame, "Quantity to add:", "1");
                try {
                    int q = Integer.parseInt(qtyStr);
                    if (q <= 0) throw new NumberFormatException();
                    if (q > p.getStock()) {
                        JOptionPane.showMessageDialog(frame, "Not enough stock. Only " + p.getStock() + " available.");
                        return;
                    }
                    cart.addItem(p, q);
                    updateCartLabel();
                    JOptionPane.showMessageDialog(frame, "Added to cart: " + q + " x " + p.getName(), "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Please enter a valid quantity.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                }
            });

            card.add(bottom, BorderLayout.SOUTH);
            return card;
        }
        void updateCartLabel() {
            cartLabel.setText("<html><b>Cart:</b> " + cart.totalItems() + " | $" + MONEY.format(cart.totalPrice()) + "</html>");
        }

        void refresh() {
            String prevCat = (String) categoryBox.getSelectedItem();
            categoryBox.removeAllItems();
            categoryBox.addItem("");
            Set<String> cats = new TreeSet<>();
            for (Product p : productController.listAll()) cats.add(p.getCategory());
            for (String c : cats) categoryBox.addItem(c);
            if (prevCat != null) categoryBox.setSelectedItem(prevCat);
            populateCards(productController.listAll());
            updateCartLabel();
        }
    }

    // AdminView: product management + reports
    class AdminView extends JPanel {
        private JTable prodTable;
        private ProductTableModel prodModel;
        AdminView(ECommerceApp app) {
            setName("ADMIN");
            setLayout(new BorderLayout(12,12));
            setBackground(BG);
            setBorder(new EmptyBorder(15,15,15,15));

            JLabel hdr = new JLabel("<html><span style='font-size:20pt;color:#2e3b4f'><b>Admin Dashboard</b></span><br><span style='font-size:11pt;color:#6b6f78'>Manage products and view orders</span></html>");
            hdr.setBorder(new EmptyBorder(0,0,10,0));
            add(hdr, BorderLayout.NORTH);

            prodModel = new ProductTableModel(productController.listAll());
            prodTable = new JTable(prodModel);
            prodTable.setRowHeight(30);
            prodTable.setFont(fontBody);
            prodTable.getTableHeader().setFont(fontBody.deriveFont(Font.BOLD));
            prodTable.setSelectionBackground(new Color(240, 245, 250));
            prodTable.setGridColor(BORDER);

            JScrollPane sp = new JScrollPane(prodTable);
            sp.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(BORDER, 1), "Products"
            ));
            sp.setPreferredSize(new Dimension(750, 350));
            add(sp, BorderLayout.CENTER);

            JPanel right = new JPanel(new GridLayout(0,1,8,8));
            right.setBackground(BG);
            right.setBorder(new EmptyBorder(0, 15, 0, 0));

            JButton add = styledButton("Add Product", ACCENT, ACCENT_DARK);
            JButton edit = styledButton("Edit Selected", BUTTON, BUTTON_HOVER);
            JButton remove = styledButton("Remove Selected", BUTTON, BUTTON_HOVER);
            JButton viewOrders = styledButton("View All Orders", BUTTON, BUTTON_HOVER);
            JButton logout = styledButton("Logout", BUTTON, BUTTON_HOVER);
            // JButton backStore = styledButton("Back to Store", BUTTON, BUTTON_HOVER); // HIDDEN

            Dimension btnSize = new Dimension(160, 36);
            add.setPreferredSize(btnSize);
            edit.setPreferredSize(btnSize);
            remove.setPreferredSize(btnSize);
            viewOrders.setPreferredSize(btnSize);
            logout.setPreferredSize(btnSize);
            // backStore.setPreferredSize(btnSize);

            right.add(add); right.add(edit); right.add(remove); right.add(viewOrders);
            // right.add(backStore); // HIDDEN
            right.add(logout);

            add(right, BorderLayout.EAST);

            add.addActionListener(e -> {
                ProductForm pf = new ProductForm(frame, null);
                pf.setVisible(true);
                if (pf.saved) {
                    productController.addProduct(pf.name, pf.desc, pf.price, pf.stock, pf.category);
                    productController.updateProduct(null);
                    refresh();
                }
            });
            edit.addActionListener(e -> {
                int r = prodTable.getSelectedRow();
                if (r < 0) {
                    JOptionPane.showMessageDialog(frame, "Please select a product to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Product p = prodModel.getProductAt(r);
                ProductForm pf = new ProductForm(frame, p);
                pf.setVisible(true);
                if (pf.saved) {
                    p.setName(pf.name); p.setDescription(pf.desc); p.setPrice(pf.price);
                    p.setStock(pf.stock); p.setCategory(pf.category);
                    productController.updateProduct(p);
                    refresh();
                }
            });
            remove.addActionListener(e -> {
                int r = prodTable.getSelectedRow();
                if (r < 0) {
                    JOptionPane.showMessageDialog(frame, "Please select a product to remove.", "No Selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Product p = prodModel.getProductAt(r);
                int yn = JOptionPane.showConfirmDialog(frame,
                        "Are you sure you want to remove '" + p.getName() + "'?",
                        "Confirm Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (yn == JOptionPane.YES_OPTION) {
                    productController.removeProduct(p.getId());
                    refresh();
                }
            });
            viewOrders.addActionListener(e -> {
                OrdersAdminDialog dlg = new OrdersAdminDialog(frame, orderController);
                dlg.setVisible(true);
                refresh();
            });
            logout.addActionListener(e -> app.logout());
            // backStore.addActionListener(e -> app.showStore()); // HIDDEN
        }

        void refresh() {
            prodModel.setProducts(productController.listAll());
        }
    }

    // OrdersView: customer or admin orders
    class OrdersView extends JPanel {
        private final JTable table;
        private final OrdersTableModel model;
        OrdersView(ECommerceApp app) {
            setName("ORDERS");
            setLayout(new BorderLayout());
            setBackground(BG);
            setBorder(new EmptyBorder(15,15,15,15));

            JLabel lbl = new JLabel("<html><span style='font-size:18pt;color:#2e3b4f'><b>Order History</b></span><br><span style='font-size:11pt;color:#6b6f78'>View your past orders and their status</span></html>");
            lbl.setBorder(new EmptyBorder(0,0,15,0));
            add(lbl, BorderLayout.NORTH);

            model = new OrdersTableModel(Collections.emptyList());
            table = new JTable(model);
            table.setRowHeight(28);
            table.setFont(fontBody);
            table.getTableHeader().setFont(fontBody.deriveFont(Font.BOLD));
            table.setSelectionBackground(new Color(240, 245, 250));
            table.setGridColor(BORDER);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(BORDER, 1), "Orders"
            ));
            add(scrollPane, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setBackground(BG);
            bottom.setBorder(new EmptyBorder(15, 0, 0, 0));
            JButton back = styledButton("Back", BUTTON, BUTTON_HOVER);
            back.setPreferredSize(new Dimension(120, 36));
            bottom.add(back);
            add(bottom, BorderLayout.SOUTH);

            back.addActionListener(e -> {
                if (currentUser != null && currentUser.getRole() == User.Role.ADMIN) showAdmin(); else showStore();
            });
        }

        void refresh() {
            if (currentUser == null) return;
            if (currentUser.getRole() == User.Role.ADMIN) model.setOrders(orderController.getAllOrders());
            else model.setOrders(orderController.getOrdersForUser(currentUser.getId()));
        }
    }

// ------------------------
// Dialogs and helpers
// ------------------------

    // ProductForm dialog for admin add/edit
    class ProductForm extends JDialog {
        boolean saved = false;
        String name, desc, category;
        double price;
        int stock;
        JTextField nameF = new JTextField(20);
        JTextField priceF = new JTextField(10);
        JTextField stockF = new JTextField(5);
        JTextField categoryF = new JTextField(10);
        JTextArea descA = new JTextArea(5,20);

        ProductForm(Frame owner, Product p) {
            super(owner, true);
            setTitle(p == null ? "Add New Product" : "Edit Product");
            setLayout(new BorderLayout(8,8));
            getContentPane().setBackground(BG);
            setResizable(false);

            JPanel center = new JPanel(new GridBagLayout());
            center.setBackground(BG);
            center.setBorder(new EmptyBorder(15,15,15,15));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8,8,8,8);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            styleTextField(nameF);
            styleTextField(priceF);
            styleTextField(stockF);
            styleTextField(categoryF);
            descA.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            descA.setBackground(Color.WHITE);
            descA.setLineWrap(true);
            descA.setWrapStyleWord(true);

            gbc.gridx = 0; gbc.gridy = 0; center.add(new JLabel("Name:"), gbc);
            gbc.gridx = 1; center.add(nameF, gbc);
            gbc.gridx = 0; gbc.gridy++; center.add(new JLabel("Price:"), gbc);
            gbc.gridx = 1; center.add(priceF, gbc);
            gbc.gridx = 0; gbc.gridy++; center.add(new JLabel("Stock:"), gbc);
            gbc.gridx = 1; center.add(stockF, gbc);
            gbc.gridx = 0; gbc.gridy++; center.add(new JLabel("Category:"), gbc);
            gbc.gridx = 1; center.add(categoryF, gbc);
            gbc.gridx = 0; gbc.gridy++; center.add(new JLabel("Description:"), gbc);
            gbc.gridx = 1; center.add(new JScrollPane(descA), gbc);

            add(center, BorderLayout.CENTER);
            JPanel bottom = new JPanel();
            bottom.setBackground(BG);
            bottom.setBorder(new EmptyBorder(10, 0, 0, 0));
            JButton save = styledButton("Save", ACCENT, ACCENT_DARK);
            JButton cancel = styledButton("Cancel", BUTTON, BUTTON_HOVER);
            save.setPreferredSize(new Dimension(100, 34));
            cancel.setPreferredSize(new Dimension(100, 34));
            bottom.add(save); bottom.add(cancel);
            add(bottom, BorderLayout.SOUTH);

            if (p != null) {
                nameF.setText(p.getName());
                priceF.setText(Double.toString(p.getPrice()));
                stockF.setText(Integer.toString(p.getStock()));
                categoryF.setText(p.getCategory());
                descA.setText(p.getDescription());
            }

            save.addActionListener(e -> {
                try {
                    name = nameF.getText().trim();
                    price = Double.parseDouble(priceF.getText().trim());
                    stock = Integer.parseInt(stockF.getText().trim());
                    category = categoryF.getText().trim();
                    desc = descA.getText().trim();
                    if (name.isEmpty()) throw new IllegalArgumentException("Product name is required");
                    if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
                    if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative");
                    saved = true;
                    dispose();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter valid numbers for price and stock.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            cancel.addActionListener(e -> dispose());
            pack();
            setLocationRelativeTo(owner);
        }
    }

    // Cart model for session
    static class Cart {
        private final Map<UUID, CartItem> items = new LinkedHashMap<>();

        void addItem(Product p, int qty) {
            CartItem ci = items.get(p.getId());
            if (ci == null) items.put(p.getId(), new CartItem(p.getId(), p.getName(), p.getPrice(), qty));
            else ci.quantity += qty;
        }

        void removeItem(UUID pid) { items.remove(pid); }
        void setQty(UUID pid, int qty) {
            CartItem ci = items.get(pid);
            if (ci != null) {
                if (qty <= 0) items.remove(pid);
                else ci.quantity = qty;
            }
        }
        List<CartItem> getItems() { return new ArrayList<>(items.values()); }
        int totalItems() { return items.values().stream().mapToInt(ci -> ci.quantity).sum(); }
        double totalPrice() { return items.values().stream().mapToDouble(ci -> ci.price * ci.quantity).sum(); }
        void clear() { items.clear(); }
    }

    static class CartItem implements Serializable {
        private static final long serialVersionUID = 1L;
        final UUID productId;
        final String productName;
        final double price;
        int quantity;
        CartItem(UUID pid, String name, double price, int qty) {
            this.productId = pid; this.productName = name; this.price = price; this.quantity = qty;
        }
    }

    // CartDialog: view contents and checkout
    class CartDialog extends JDialog {
        CartDialog(Frame owner, Cart cart, ProductController pc, OrderController oc, ECommerceApp app) {
            super(owner, "Shopping Cart", true);
            setLayout(new BorderLayout(8,8));
            getContentPane().setBackground(BG);
            setResizable(false);

            List<CartItem> items = cart.getItems();
            String[] cols = { "Product", "Unit Price", "Qty", "Subtotal" };
            Object[][] data = new Object[items.size()][4];
            for (int i = 0; i < items.size(); i++) {
                CartItem it = items.get(i);
                data[i][0] = it.productName;
                data[i][1] = "$" + MONEY.format(it.price);
                data[i][2] = it.quantity;
                data[i][3] = "$" + MONEY.format(it.price * it.quantity);
            }

            JTable tbl = new JTable(data, cols);
            tbl.setRowHeight(28);
            tbl.setFont(fontBody);
            tbl.getTableHeader().setFont(fontBody.deriveFont(Font.BOLD));
            tbl.setEnabled(false);

            JScrollPane sp = new JScrollPane(tbl);
            sp.setPreferredSize(new Dimension(520, 240));
            sp.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(BORDER, 1), "Cart Items"
            ));
            add(sp, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setBackground(BG);
            bottom.setBorder(new EmptyBorder(10, 0, 0, 0));

            JLabel totalLbl = new JLabel("<html><b>Total: $" + MONEY.format(cart.totalPrice()) + "</b></html>");
            totalLbl.setFont(fontSubtitle);
            totalLbl.setForeground(ACCENT_DARK);
            bottom.add(totalLbl, BorderLayout.WEST);

            JPanel buttons = new JPanel();
            buttons.setBackground(BG);
            JButton checkout = styledButton("Checkout", ACCENT, ACCENT_DARK);
            JButton close = styledButton("Close", BUTTON, BUTTON_HOVER);
            checkout.setPreferredSize(new Dimension(120, 36));
            close.setPreferredSize(new Dimension(100, 36));
            buttons.add(checkout);
            buttons.add(close);
            bottom.add(buttons, BorderLayout.EAST);

            add(bottom, BorderLayout.SOUTH);

            checkout.addActionListener(e -> {
                if (app.getCurrentUser() == null) {
                    JOptionPane.showMessageDialog(this, "Please login to place your order.", "Login Required", JOptionPane.WARNING_MESSAGE);
                    dispose();
                    app.showAuth();
                    return;
                }

                if (items.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Your cart is empty. Add some items before checkout.", "Empty Cart", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                int ok = JOptionPane.showConfirmDialog(this,
                        "Confirm order for $" + MONEY.format(cart.totalPrice()) + "?",
                        "Confirm Checkout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ok != JOptionPane.YES_OPTION) return;

                List<OrderItem> oitems = new ArrayList<>();
                for (CartItem ci : cart.getItems()) {
                    oitems.add(new OrderItem(ci.productId, ci.productName, ci.quantity, ci.price));
                }
                try {
                    Order ord = oc.placeOrder(app.getCurrentUser(), oitems);
                    JOptionPane.showMessageDialog(this,
                            "<html><b>Order placed successfully!</b><br>Order ID: " + ord.getId().toString().substring(0, 8) + "...<br>Total: $" + MONEY.format(ord.getTotal()) + "</html>",
                            "Order Confirmed", JOptionPane.INFORMATION_MESSAGE);
                    cart.clear();
                    dispose();
                    app.showStore();
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, "Error placing order: " + ex.getMessage(), "Order Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            close.addActionListener(e -> dispose());
            pack();
            setLocationRelativeTo(owner);
        }
    }

    // OrdersAdminDialog: admin view of orders
    class OrdersAdminDialog extends JDialog {
        OrdersAdminDialog(Frame owner, OrderController oc) {
            super(owner, "Order Management", true);
            setLayout(new BorderLayout());
            getContentPane().setBackground(BG);

            List<Order> orders = oc.getAllOrders();
            OrdersTableModel model = new OrdersTableModel(orders);
            JTable tbl = new JTable(model);
            tbl.setRowHeight(28);
            tbl.setFont(fontBody);
            tbl.getTableHeader().setFont(fontBody.deriveFont(Font.BOLD));
            tbl.setSelectionBackground(new Color(240, 245, 250));

            JScrollPane scrollPane = new JScrollPane(tbl);
            scrollPane.setBorder(new EmptyBorder(10,10,10,10));
            add(scrollPane, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setBackground(BG);
            bottom.setBorder(new EmptyBorder(10, 10, 10, 10));
            JButton setShipped = styledButton("Mark Shipped", BUTTON, BUTTON_HOVER);
            JButton setDelivered = styledButton("Mark Delivered", BUTTON, BUTTON_HOVER);
            JButton close = styledButton("Close", BUTTON, BUTTON_HOVER);

            setShipped.setPreferredSize(new Dimension(140, 36));
            setDelivered.setPreferredSize(new Dimension(140, 36));
            close.setPreferredSize(new Dimension(100, 36));

            bottom.add(setShipped);
            bottom.add(setDelivered);
            bottom.add(close);
            add(bottom, BorderLayout.SOUTH);

            setShipped.addActionListener(e -> {
                int r = tbl.getSelectedRow();
                if (r < 0) {
                    JOptionPane.showMessageDialog(this, "Please select an order.", "No Selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Order o = model.getOrderAt(r);
                oc.updateOrderStatus(o.getId(), Order.Status.SHIPPED);
                model.setOrders(oc.getAllOrders());
                JOptionPane.showMessageDialog(this, "Order marked as shipped.", "Status Updated", JOptionPane.INFORMATION_MESSAGE);
            });

            setDelivered.addActionListener(e -> {
                int r = tbl.getSelectedRow();
                if (r < 0) {
                    JOptionPane.showMessageDialog(this, "Please select an order.", "No Selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Order o = model.getOrderAt(r);
                oc.updateOrderStatus(o.getId(), Order.Status.DELIVERED);
                model.setOrders(oc.getAllOrders());
                JOptionPane.showMessageDialog(this, "Order marked as delivered.", "Status Updated", JOptionPane.INFORMATION_MESSAGE);
            });

            close.addActionListener(e -> dispose());
            setSize(900, 500);
            setLocationRelativeTo(owner);
        }
    }

    // ------------------------
// Table models for product & orders
// ------------------------
    static class ProductTableModel extends AbstractTableModel {
        private List<Product> products;
        private final String[] cols = { "Name", "Category", "Price", "Stock", "Description" };
        ProductTableModel(List<Product> list) { products = new ArrayList<>(list); }

        public void setProducts(List<Product> list) {
            products = new ArrayList<>(list);
            fireTableDataChanged();
        }

        public Product getProductAt(int r) { return products.get(r); }

        @Override public int getRowCount() { return products.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            Product p = products.get(r);
            switch (c) {
                case 0: return p.getName();
                case 1: return p.getCategory();
                case 2: return "$" + MONEY.format(p.getPrice());
                case 3: return p.getStock();
                case 4: return p.getDescription();
                default: return "";
            }
        }
    }

    static class OrdersTableModel extends AbstractTableModel {
        private List<Order> orders;
        private final String[] cols = { "Order ID", "User ID", "Created", "Items", "Total", "Status" };
        OrdersTableModel(List<Order> list) { orders = new ArrayList<>(list); }

        public void setOrders(List<Order> list) {
            orders = new ArrayList<>(list);
            fireTableDataChanged();
        }

        public Order getOrderAt(int r) { return orders.get(r); }

        @Override public int getRowCount() { return orders.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            Order o = orders.get(r);
            switch (c) {
                case 0: return o.getId().toString().substring(0, 8) + "...";
                case 1: return o.getUserId().toString().substring(0, 8) + "...";
                case 2: return o.getCreatedAt().toString().replace("T", " ");
                case 3: return o.getItems().size();
                case 4: return "$" + MONEY.format(o.getTotal());
                case 5: return o.getStatus().toString();
                default: return "";
            }
        }
    }

// ------------------------
// UI utility helpers
// ------------------------

    // Styled button with hover effect (modern: accent buttons have white foreground)
    private JButton styledButton(String text, Color bg, Color hoverBg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(hoverBg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(hoverBg);
                } else {
                    g2.setColor(bg);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();

                super.paintComponent(g);
            }
        };

        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        // choose foreground based on bg - accent buttons get white text
        if (bg.equals(ACCENT) || bg.equals(ACCENT_DARK)) b.setForeground(Color.WHITE);
        else b.setForeground(TEXT);
        b.setFont(fontBody);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        return b;
    }

    // Overload for default button style
    private JButton styledButton(String text) {
        return styledButton(text, BUTTON, BUTTON_HOVER);
    }

    private void styleTextField(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setBackground(Color.WHITE);
        field.setFont(fontBody);
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setBackground(Color.WHITE);
        combo.setFont(fontBody);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    /**
     * WrapLayout: FlowLayout that wraps components to a new row when necessary.
     */
    static class WrapLayout extends FlowLayout {
        public WrapLayout() { super(); }
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension d = layoutSize(target, false);
            d.width -= (getHgap() + 1);
            return d;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                Insets insets = target.getInsets();
                int hgap = getHgap(), vgap = getVgap();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap*2);

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;

                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0; rowHeight = 0;
                    }
                    if (rowWidth != 0) rowWidth += hgap;
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                addRow(dim, rowWidth, rowHeight);
                dim.width += insets.left + insets.right + hgap*2;
                dim.height += insets.top + insets.bottom + vgap*2;
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            if (rowWidth > 0) {
                dim.width = Math.max(dim.width, rowWidth);
                if (dim.height > 0) dim.height += getVgap();
                dim.height += rowHeight;
            }
        }
    }

    /**
     * DropShadowBorder: Creates a subtle shadow effect for components
     */
    static class DropShadowBorder extends AbstractBorder {
        private final Color shadowColor;
        private final int shadowSize;
        private final float shadowOpacity;
        private final int cornerSize;
        private final boolean showTopShadow;
        private final boolean showLeftShadow;
        private final boolean showBottomShadow;
        private final boolean showRightShadow;

        public DropShadowBorder(Color shadowColor, int shadowSize, float shadowOpacity,
                                int cornerSize, boolean showTopShadow, boolean showLeftShadow,
                                boolean showBottomShadow, boolean showRightShadow) {
            this.shadowColor = shadowColor;
            this.shadowSize = shadowSize;
            this.shadowOpacity = shadowOpacity;
            this.cornerSize = cornerSize;
            this.showTopShadow = showTopShadow;
            this.showLeftShadow = showLeftShadow;
            this.showBottomShadow = showBottomShadow;
            this.showRightShadow = showRightShadow;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color shadow = new Color(shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(),
                    (int)(255 * shadowOpacity));
            g2.setColor(shadow);

            if (showBottomShadow) {
                g2.fillRoundRect(x + shadowSize, y + height - shadowSize/2, width - shadowSize * 2, shadowSize, cornerSize, cornerSize);
            }
            if (showRightShadow) {
                g2.fillRoundRect(x + width - shadowSize/2, y + shadowSize, shadowSize, height - shadowSize * 2, cornerSize, cornerSize);
            }

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            int top = showTopShadow ? shadowSize : 0;
            int left = showLeftShadow ? shadowSize : 0;
            int bottom = showBottomShadow ? shadowSize : 0;
            int right = showRightShadow ? shadowSize : 0;
            return new Insets(top, left, bottom, right);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
