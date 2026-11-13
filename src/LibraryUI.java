import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.*;
import javax.swing.text.JTextComponent; 


public class LibraryUI {

    //THEMESETTINGS
    static final class Theme {
        final Color bg, surface, border, text, muted, accent, accentTint, onAccent,
                    okFg, okBg, warnFg, warnBg, offFg, offBg, danger;

        Theme(String... h) {
            Color[] c = new Color[h.length];
            for (int i = 0; i < h.length; i++) c[i] = Color.decode(h[i]);
            bg = c[0]; surface = c[1]; border = c[2]; text = c[3]; muted = c[4];
            accent = c[5]; accentTint = c[6]; onAccent = c[7];
            okFg = c[8]; okBg = c[9]; warnFg = c[10]; warnBg = c[11];
            offFg = c[12]; offBg = c[13]; danger = c[14];
        }

        static final Theme LIGHT = new Theme(
            "#F6F9F6", "#FFFFFF", "#E2E8E3", "#16201A", "#66756B", "#438359", "#EAF3EC", "#FFFFFF",
            "#2F7048", "#E3F1E7", "#8A6414", "#FBF1D9", "#6F7A73", "#EDF0EE", "#B56A65");
        static final Theme DARK = new Theme(
            "#0C141D", "#121F2B", "#243646", "#E8F0F5", "#889DAB", "#5EBACE", "#203E4B", "#0A161E",
            "#5EBACE", "#1D3E4B", "#E2BE6E", "#393225", "#889DAB", "#243646", "#D48580");

        private static Theme current = LIGHT;
        static Theme cur() { return current; }
        static boolean dark() { return current == DARK; }
    }

    //THEMESWITCH
    interface R {
        Function<Theme, Color> BG = t -> t.bg, SURFACE = t -> t.surface, BORDER = t -> t.border,
            TEXT = t -> t.text, MUTED = t -> t.muted, ACCENT = t -> t.accent,
            TINT = t -> t.accentTint, DANGER = t -> t.danger;
    }

    static final String FAMILY = pickFamily("Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue", "SansSerif");

    static String pickFamily(String... wanted) {
        Set<String> have = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : wanted) if (have.contains(f)) return f;
        return "SansSerif";
    }

    static Font font(float size, boolean bold) {
        return new Font(FAMILY, bold ? Font.BOLD : Font.PLAIN, 1).deriveFont(size);
    }

    static Graphics2D g2(Graphics g) {
        Graphics2D x = (Graphics2D) g.create();
        x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        x.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return x;
    }

    static void box(Graphics2D g, int w, int h, int arc, Color fill, Color stroke) {
        if (fill != null) { g.setColor(fill); g.fillRoundRect(0, 0, w, h, arc, arc); }
        if (stroke != null) { g.setColor(stroke); g.drawRoundRect(0, 0, w - 1, h - 1, arc, arc); }
    }

    static Color mix(Color a, Color b, float f) {
        return new Color(
            Math.round(a.getRed() + (b.getRed() - a.getRed()) * f),
            Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * f),
            Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * f));
    }

    static final Cursor HAND = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    //LAYOUTBUILDER
    static class Surface extends JPanel {
        final Function<Theme, Color> fill, stroke;
        final int radius;
        char edge = 0;

        Surface(LayoutManager lm, Function<Theme, Color> fill, Function<Theme, Color> stroke, int radius) {
            super(lm);
            this.fill = fill; this.stroke = stroke; this.radius = radius;
            setOpaque(radius == 0 && stroke == null && fill != null);
        }
        Surface edge(char e) { edge = e; return this; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g); Theme t = Theme.cur();
            box(g2, getWidth(), getHeight(), radius * 2,
                fill == null ? null : fill.apply(t), stroke == null ? null : stroke.apply(t));
            g2.setColor(t.border);
            if (edge == 'r') g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            if (edge == 'b') g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            g2.dispose();
        }
    }

    static Surface card(LayoutManager lm) { return new Surface(lm, R.SURFACE, R.BORDER, 12); }
    static class VBox extends JPanel {
        final float align;
        VBox() { this(false); }
        VBox(boolean center) {
            align = center ? 0.5f : 0f;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }
        VBox put(JComponent c) { c.setAlignmentX(align); add(c); return this; }
        VBox gap(int h) { add(Box.createVerticalStrut(h)); return this; }
    }

    static class Label extends JLabel {
        final Function<Theme, Color> role;
        Label(String s, float size, boolean bold, Function<Theme, Color> role) {
            super(s); this.role = role; setFont(font(size, bold));
        }
        @Override public Color getForeground() {
            return role == null ? super.getForeground() : role.apply(Theme.cur());
        }
    }

    static class Pill extends Label {
        final Function<Theme, Color> bg;
        Pill(String s, Function<Theme, Color> fg, Function<Theme, Color> bg) {
            super(s, 11, true, fg); this.bg = bg;
            setBorder(new EmptyBorder(3, 10, 3, 10));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g);
            g2.setColor(bg.apply(Theme.cur()));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static Pill statusPill(Book b) {
        switch (b.status()) {
            case "Available":       return new Pill("• Available", t -> t.okFg, t -> t.okBg);
            case "Borrowed by you": return new Pill("• Borrowed by you", t -> t.warnFg, t -> t.warnBg);
            default:                return new Pill("• Unavailable", t -> t.offFg, t -> t.offBg);
        }
    }

    //SINGLEBTN
    static class Btn extends JButton {
        enum Kind { PRIMARY, OUTLINE, GHOST, NAV, TAB, DISABLED, DANGER }
        final Kind kind;
        boolean active, hover, left, chevron;

        Btn(String s, Kind k) {
            super(s); kind = k;
            setFont(font(13, k == Kind.PRIMARY));
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setCursor(HAND);
            left = k == Kind.NAV;
            setBorder(new EmptyBorder(9, k == Kind.NAV ? 12 : 16, 9, k == Kind.NAV ? 12 : 16));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        Btn active(boolean a) { active = a; repaint(); return this; }
        Btn size(int w, int h) { setPreferredSize(new Dimension(w, h)); return this; }

        @Override public Dimension getPreferredSize() {
            if (isPreferredSizeSet()) return super.getPreferredSize();
            FontMetrics fm = getFontMetrics(getFont()); Insets in = getInsets();
            return new Dimension(fm.stringWidth(getText()) + in.left + in.right + (chevron ? 18 : 0),
                                 fm.getHeight() + in.top + in.bottom);
        }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = g2(gr); Theme t = Theme.cur();
            Color fill = null, stroke = null, fg = t.text;
            switch (kind) {
                case PRIMARY:  fill = hover ? mix(t.accent, Color.BLACK, .10f) : t.accent; fg = t.onAccent; break;
                case OUTLINE:  fill = hover ? t.accentTint : t.surface; stroke = t.border; break;
                case GHOST:    fg = t.accent; break;
                case DANGER:   fg = t.danger; break;
                case DISABLED: fill = t.offBg; fg = t.offFg; break;
                default:       fill = active ? t.accentTint : hover ? mix(t.bg, t.accentTint, .6f) : null;
                               fg = active ? t.accent : t.muted;
            }
            box(g, getWidth(), getHeight(), 16, fill, stroke);
            g.setFont(getFont()); g.setColor(fg);
            FontMetrics fm = g.getFontMetrics(); Insets in = getInsets();
            int sw = fm.stringWidth(getText());
            int x = left ? in.left : (getWidth() - sw) / 2;
            g.drawString(getText(), x, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            if (chevron) {
                g.setStroke(new BasicStroke(1.6f)); g.setColor(t.muted);
                int cx = getWidth() - in.right - 6, cy = getHeight() / 2;
                g.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
            }
            g.dispose();
        }
    }


    static class Dropdown extends Btn {
        final String[] options; String value; final Consumer<String> onChange;
        Dropdown(String[] options, Consumer<String> onChange) {
            super(options[0], Kind.OUTLINE);
            this.options = options; this.value = options[0]; this.onChange = onChange;
            left = true; chevron = true;
            setPreferredSize(new Dimension(170, 40));
            addActionListener(e -> open());
        }
        void set(String s) { value = s; setText(s); repaint(); }
        void open() {
            Theme t = Theme.cur();
            JPopupMenu m = new JPopupMenu();
            m.setBackground(t.surface);
            m.setBorder(BorderFactory.createLineBorder(t.border));
            for (String o : options) {
                JMenuItem it = new JMenuItem(o);
                it.setOpaque(true); it.setBackground(t.surface); it.setForeground(t.text);
                it.setFont(font(13, false));
                it.addActionListener(e -> { set(o); onChange.accept(o); });
                m.add(it);
            }
            m.setPreferredSize(new Dimension(Math.max(getWidth(), 150), options.length * 28 + 4));
            m.show(this, 0, getHeight() + 4);
        }
    }

    static void paintField(Graphics g, JTextComponent c, String hint, Runnable paintText) {
        Graphics2D g2 = g2(g); Theme t = Theme.cur();
        box(g2, c.getWidth(), c.getHeight(), 16, t.surface, c.hasFocus() ? t.accent : t.border);
        g2.dispose();
        paintText.run();
        if (c.getText().isEmpty()) {
            Graphics2D h = g2(g);
            h.setFont(c.getFont()); h.setColor(t.muted);
            FontMetrics fm = h.getFontMetrics();
            h.drawString(hint, c.getInsets().left, (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            h.dispose();
        }
    }

    static void styleField(JTextComponent c) {
        c.setOpaque(false);
        c.setBorder(new EmptyBorder(0, 12, 0, 12));
        c.setFont(font(13, false));
        c.setPreferredSize(new Dimension(200, 40));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        c.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { c.repaint(); }
            @Override public void focusLost(FocusEvent e) { c.repaint(); }
        });
    }

    static class Field extends JTextField {
        final String hint;
        Field(String hint) { this.hint = hint; styleField(this); }
        @Override public Color getForeground() { return Theme.cur().text; }
        @Override public Color getCaretColor() { return Theme.cur().text; }
        @Override protected void paintComponent(Graphics g) { paintField(g, this, hint, () -> super.paintComponent(g)); }
    }

    static class PField extends JPasswordField {
        PField(String hint) { this.hint = hint; styleField(this); }
        final String hint;
        @Override public Color getForeground() { return Theme.cur().text; }
        @Override public Color getCaretColor() { return Theme.cur().text; }
        @Override protected void paintComponent(Graphics g) { paintField(g, this, hint, () -> super.paintComponent(g)); }
    }

    static class Check extends JComponent {
        boolean on; final String text;
        Check(String text) {
            this.text = text; setFont(font(12, false)); setCursor(HAND);
            addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { on = !on; repaint(); } });
        }
        @Override public Dimension getPreferredSize() { return new Dimension(24 + getFontMetrics(getFont()).stringWidth(text), 20); }
        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = g2(gr); Theme t = Theme.cur();
            g.translate(0, 3);
            box(g, 14, 14, 8, on ? t.accent : t.surface, on ? t.accent : t.border);
            if (on) { g.setColor(t.onAccent); g.setStroke(new BasicStroke(1.8f)); g.drawPolyline(new int[]{3, 6, 11}, new int[]{7, 10, 4}, 3); }
            g.translate(0, -3);
            g.setFont(getFont()); g.setColor(t.muted);
            g.drawString(text, 22, (getHeight() - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent());
            g.dispose();
        }
    }


    static class Logo extends JComponent {
        private BufferedImage cache; private int cd;
        Logo(int d) { Dimension s = new Dimension(d, d); setPreferredSize(s); setMaximumSize(s); setMinimumSize(s); }
        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = g2(gr); Theme t = Theme.cur(); int d = getWidth();
            BufferedImage src = loadImage("C:\\Users\\desiree\\OneDrive\\Desktop\\LIBRARYSYSTEMJAVA\\IMG_DIR\\Icon\\DEIHS Logo.jpg");
            if (src != null) {
                if (cache == null || cd != d) { cache = masked(src, d, d, d); cd = d; }
                g.drawImage(cache, 0, 0, null);
            } else {
                g.setColor(t.accent); g.fillOval(0, 0, d, d);
                g.setColor(t.onAccent); g.setFont(font(d * 0.46f, true));
                FontMetrics fm = g.getFontMetrics();
                g.drawString("D", (d - fm.stringWidth("D")) / 2, (d - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }

    /** Tinted square used as the icon tile on stat cards. Swap paintComponent for an SVG icon later. */
    static class Chip extends JComponent {
        Chip() { Dimension s = new Dimension(32, 32); setPreferredSize(s); setMaximumSize(s); setMinimumSize(s); }
        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = g2(gr); Theme t = Theme.cur();
            box(g, 32, 32, 16, t.accentTint, null);
            g.setColor(t.accent); g.setStroke(new BasicStroke(1.6f)); g.drawRoundRect(10, 9, 12, 14, 4, 4);
            g.dispose();
        }
    }

    static class Meter extends JComponent {
        final double value;
        Meter(double v) { value = v; setPreferredSize(new Dimension(100, 8)); setMaximumSize(new Dimension(Integer.MAX_VALUE, 8)); }
        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = g2(gr); Theme t = Theme.cur(); int w = getWidth(), h = getHeight();
            box(g, w, h, h, t.accentTint, null);
            box(g, Math.max(h, (int) (w * value)), h, h, t.accent, null);
            g.dispose();
        }
    }
    
    //IMAGECREATOR
    static final Path IMG_DIR = Paths.get(System.getProperty("app.img", "img")).toAbsolutePath().normalize();
    static final Map<String, BufferedImage> IMAGES = new HashMap<>();

    static BufferedImage loadImage(String relPath) {
        if (relPath == null || relPath.trim().isEmpty()) return null;
        if (IMAGES.containsKey(relPath)) return IMAGES.get(relPath);
        BufferedImage img = null;
        try {
            Path p = IMG_DIR.resolve(relPath).normalize();
            if (p.startsWith(IMG_DIR) && Files.isRegularFile(p)) img = ImageIO.read(p.toFile());
        } catch (Exception ignored) { }
        IMAGES.put(relPath, img);
        return img;
    }

    //RETURNSFILEPATH
    static String saveCover(File src, String title) throws java.io.IOException {
        String name = src.getName(); int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot).toLowerCase() : ".jpg";
        String slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (slug.isEmpty()) slug = "book";
        String file = slug + "-" + Long.toString(System.currentTimeMillis(), 36) + ext;
        Path dir = IMG_DIR.resolve("covers");
        Files.createDirectories(dir);
        Files.copy(src.toPath(), dir.resolve(file), StandardCopyOption.REPLACE_EXISTING);
        return "covers/" + file;
    }

    static BufferedImage masked(BufferedImage src, int w, int h, int arc) {
        BufferedImage out = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.fillRoundRect(0, 0, w, h, arc, arc);
        g.setComposite(AlphaComposite.SrcIn);
        double k = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
        int dw = (int) Math.ceil(src.getWidth() * k), dh = (int) Math.ceil(src.getHeight() * k);
        g.drawImage(src, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        g.dispose();
        return out;
    }

    //BOOKCOVER
    static class Cover extends JComponent {
        final Book b; final boolean thumb;
        private BufferedImage cache; private int cw, ch;
        Cover(Book b, boolean thumb) { this.b = b; this.thumb = thumb; }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = g2(gr); int w = getWidth(), h = getHeight(); float hue = b.hue / 360f;
            int arc = thumb ? 8 : 16;
            BufferedImage src = loadImage(b.coverPath);
            if (src != null) {
                if (cache == null || cw != w || ch != h) { cache = masked(src, w, h, arc); cw = w; ch = h; }
                g.drawImage(cache, 0, 0, null);
            } else {
                g.setPaint(new GradientPaint(0, 0, Color.getHSBColor(hue, .55f, .66f), 0, h, Color.getHSBColor(hue, .62f, .30f)));
                g.fillRoundRect(0, 0, w, h, arc, arc);
                if (!thumb) {
                    g.setColor(new Color(255, 255, 255, 235));
                    g.setFont(new Font(Font.SERIF, Font.BOLD, 17));
                    wrap(g, b.title, w, (int) (h * 0.30));
                    g.setFont(font(10, false)); g.setColor(new Color(255, 255, 255, 190));
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(b.author, (w - fm.stringWidth(b.author)) / 2, h - 14);
                }
            }
            if (!thumb) {   // "Printed" / "E-book" tag sits on top of either version
                g.setFont(font(10, true)); FontMetrics fm = g.getFontMetrics();
                int pw = fm.stringWidth(b.type) + 16;
                g.setColor(new Color(255, 255, 255, 235)); g.fillRoundRect(8, 8, pw, 20, 20, 20);
                g.setColor(new Color(36, 48, 42)); g.drawString(b.type, 16, 8 + (20 - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
        static void wrap(Graphics2D g, String s, int w, int y) {
            FontMetrics fm = g.getFontMetrics(); StringBuilder line = new StringBuilder();
            for (String word : s.split(" ")) {
                String test = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(test) > w - 28 && line.length() > 0) {
                    g.drawString(line.toString(), (w - fm.stringWidth(line.toString())) / 2, y);
                    y += fm.getHeight(); line = new StringBuilder(word);
                } else line = new StringBuilder(test);
            }
            g.drawString(line.toString(), (w - fm.stringWidth(line.toString())) / 2, y);
        }
    }

    //SCROLLBAR
    static class Bar extends BasicScrollBarUI {
        JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g2 = g2(g); Theme t = Theme.cur();
            g2.setColor(mix(t.border, t.muted, .35f));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    static class Tall extends VBox implements Scrollable {
        Tall() { setBorder(new EmptyBorder(28, 36, 36, 36)); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(40, r.height - 40); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null); sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        JScrollBar vb = sp.getVerticalScrollBar();
        vb.setUI(new Bar()); vb.setOpaque(false); vb.setPreferredSize(new Dimension(10, 0));
        return sp;
    }

    static <T extends JComponent> T full(T c) {
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    static JComponent hero(String title, String sub, JComponent right) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
        p.add(new VBox().put(new Label(title, 26, true, R.TEXT)).gap(4).put(new Label(sub, 13, false, R.MUTED)), BorderLayout.WEST);
        if (right != null) { JPanel r = new JPanel(new GridBagLayout()); r.setOpaque(false); r.add(right); p.add(r, BorderLayout.EAST); }
        return p;
    }

    //DATA
    static final class Book {
        int id;             
        String title, author, category, type, isbn;
        int copies, available, hue; boolean mine;
        String coverPath;   
        Book(String title, String author, String category, String type, int copies, int available, int hue, boolean mine, String isbn) {
            this.title = title; this.author = author; this.category = category; this.type = type;
            this.copies = copies; this.available = available; this.hue = hue; this.mine = mine; this.isbn = isbn;
        }
        String status() { return mine ? "Borrowed by you" : available > 0 ? "Available" : "Unavailable"; }
    }

    static final class Library {
        final List<Book> books = new ArrayList<>();
        final List<Runnable> listeners = new ArrayList<>();
        void changed() { for (Runnable r : listeners) r.run(); }
        int total() { int n = 0; for (Book b : books) n += b.copies; return n; }
        int available() { int n = 0; for (Book b : books) n += b.available; return n; }
        int copiesOf(String type) { int n = 0; for (Book b : books) if (b.type.equals(type)) n += b.copies; return n; }
    }

    final Library lib = new Library();

    //DBCONNECTION

    static final class Db {
    
        static final String URL  = System.getProperty("db.url",  "jdbc:mysql://localhost:3306/library_books?useSSL=false&serverTimezone=UTC");
        static final String USER = System.getProperty("tetora", "root");
        static final String PASS = System.getProperty("TetraPhiza666", "");

        static {
            try { Class.forName("com.mysql.cj.jdbc.Driver"); }           
            catch (ClassNotFoundException ignored) { }
        }

        static Connection connect() throws SQLException {
            return DriverManager.getConnection(URL, USER, PASS);
        }

        static int hueOf(String title) { return Math.floorMod(title.hashCode(), 360); }

        static List<Book> fetchBooks(Integer currentUserId) throws SQLException {
            String sql =
                "SELECT b.book_id, b.title, b.author, b.category, b.type, b.isbn, b.total_copies, b.cover_path, " +
                "       b.total_copies - COALESCE(active.cnt, 0) AS available, " +
                "       COALESCE(mine.cnt, 0) AS mine_count " +
                "FROM books b " +
                "LEFT JOIN (SELECT book_id, COUNT(*) cnt FROM loans WHERE returned_at IS NULL GROUP BY book_id) active " +
                "       ON active.book_id = b.book_id " +
                "LEFT JOIN (SELECT book_id, COUNT(*) cnt FROM loans WHERE returned_at IS NULL AND user_id = ? GROUP BY book_id) mine " +
                "       ON mine.book_id = b.book_id " +
                "ORDER BY b.book_id";

            List<Book> out = new ArrayList<>();
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, currentUserId == null ? -1 : currentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String title = rs.getString("title");
                        Book b = new Book(title, rs.getString("author"), rs.getString("category"),
                                           rs.getString("type"), rs.getInt("total_copies"), rs.getInt("available"),
                                           hueOf(title), rs.getInt("mine_count") > 0, rs.getString("isbn"));
                        b.id = rs.getInt("book_id");
                        b.coverPath = rs.getString("cover_path");   
                        out.add(b);
                    }
                }
            }
            return out;
        }

        static String[] authenticate(String username, String password) throws SQLException {
            String sql = "SELECT user_id, full_name, role, password_hash FROM users WHERE username = ?";
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    return new String[]{ String.valueOf(rs.getInt("user_id")), rs.getString("full_name"), rs.getString("role") };
                }
            }
        }

        //INSERTBOOKS
        static void insertBook(Book b) throws SQLException {
            String sql = "INSERT INTO books (title, author, category, type, isbn, total_copies, cover_path) VALUES (?,?,?,?,?,?,?)";
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, b.title); ps.setString(2, b.author); ps.setString(3, b.category); ps.setString(4, b.type);
                if (b.isbn == null || b.isbn.isEmpty()) ps.setNull(5, java.sql.Types.VARCHAR); else ps.setString(5, b.isbn);
                ps.setInt(6, b.copies);
                if (b.coverPath == null) ps.setNull(7, java.sql.Types.VARCHAR); else ps.setString(7, b.coverPath);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) b.id = keys.getInt(1); }
            }
        }

        //DELETEBOOKS
        static void deleteBook(int bookId) throws SQLException {
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement("DELETE FROM books WHERE book_id = ?")) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }
        }

        //INSERTLOANS
        static void borrow(int bookId, int userId) throws SQLException {
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO loans (book_id, user_id) VALUES (?, ?)")) {
                ps.setInt(1, bookId); ps.setInt(2, userId);
                ps.executeUpdate();
            }
        }

        //UPDATELOANS
        static void returnBook(int bookId, int userId) throws SQLException {
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                    "UPDATE loans SET returned_at = NOW() WHERE book_id = ? AND user_id = ? AND returned_at IS NULL " +
                    "ORDER BY borrowed_at DESC LIMIT 1")) {
                ps.setInt(1, bookId); ps.setInt(2, userId);
                ps.executeUpdate();
            }
        }
    }

    void loadFromDb() {
        try {
            lib.books.clear();
            lib.books.addAll(Db.fetchBooks(currentUserId));
        } catch (SQLException ex) {
            lib.books.clear();
            JOptionPane.showMessageDialog(root, "Couldn't load the catalog from the database:\n" + ex.getMessage(),
                                           "Database error", JOptionPane.ERROR_MESSAGE);
        }
        lib.changed();
    }

    static String fmt(int n) { return String.format("%,d", n); }

    //TABLES
    static class Row extends JPanel {
        boolean line = true;
        Row() { super(new GridBagLayout()); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            if (line) { g.setColor(Theme.cur().border); g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1); }
        }
    }

    static Row cell(JComponent inner) {
        Row r = new Row();
        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.WEST; g.weightx = 1; g.insets = new Insets(0, 12, 0, 12);
        r.add(inner, g);
        return r;
    }

    static class BookModel extends AbstractTableModel {
        final String[] cols; List<Book> rows = new ArrayList<>();
        BookModel(String... cols) { this.cols = cols; }
        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        public Object getValueAt(int r, int c) { return rows.get(r); }
    }

    static class Cells implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            Book b = (Book) v; JComponent x;
            switch (t.getColumnName(c)) {
                case "Title": {
                    JPanel p = new JPanel(new GridBagLayout()); p.setOpaque(false);
                    Cover cv = new Cover(b, true); cv.setPreferredSize(new Dimension(30, 42));
                    GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(0, 0, 0, 12); p.add(cv, g);
                    g.gridx = 1; g.insets = new Insets(0, 0, 0, 0);
                    p.add(new VBox().put(new Label(b.title, 13, true, R.TEXT)).put(new Label(b.author, 12, false, R.MUTED)), g);
                    x = p; break;
                }
                case "Catalog": x = new Label(b.category, 12, false, R.MUTED); break;
                case "Type":    x = new Label(b.type, 12, false, R.TEXT); break;
                case "Copies":  x = new Label(String.valueOf(b.copies), 12, false, R.TEXT); break;
                case "Status":  x = statusPill(b); break;
                case "Remove":  x = new Btn("×", Btn.Kind.DANGER); break;
                default:       
                    String st = b.status();
                    x = st.equals("Available") ? new Btn("Borrow", Btn.Kind.PRIMARY).size(96, 32)
                      : b.mine ? new Btn("Return", Btn.Kind.OUTLINE).size(96, 32)
                      : new Btn("Unavailable", Btn.Kind.DISABLED).size(96, 32);
            }
            return cell(x);
        }
    }

    static final Map<String, Integer> WIDTHS = Map.of(
        "Title", 320, "Catalog", 140, "Type", 90, "Status", 150, "Action", 120, "Copies", 80, "Remove", 90);

    static JComponent tableCard(BookModel m, BiConsumer<String, Book> onClick) {
        JTable t = new JTable(m);
        t.setOpaque(false); t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0, 0));
        t.setRowHeight(60); t.setFocusable(false); t.setRowSelectionAllowed(false);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        t.setDefaultRenderer(Object.class, new Cells());
        for (int i = 0; i < m.cols.length; i++) t.getColumnModel().getColumn(i).setPreferredWidth(WIDTHS.get(m.cols[i]));

        JTableHeader h = t.getTableHeader();
        h.setOpaque(false); h.setReorderingAllowed(false); h.setResizingAllowed(false);
        h.setPreferredSize(new Dimension(1, 44));
        h.setDefaultRenderer((tb, v, s, f, r, c) -> cell(new Label(String.valueOf(v), 12, true, R.MUTED)));

        MouseAdapter ma = new MouseAdapter() {
            boolean clickable(MouseEvent e) {
                int r = t.rowAtPoint(e.getPoint()), c = t.columnAtPoint(e.getPoint());
                if (r < 0 || c < 0) return false;
                String n = t.getColumnName(c);
                return n.equals("Remove") || (n.equals("Action") && !m.rows.get(r).status().equals("Unavailable"));
            }
            @Override public void mouseMoved(MouseEvent e) { t.setCursor(clickable(e) ? HAND : Cursor.getDefaultCursor()); }
            @Override public void mouseClicked(MouseEvent e) {
                if (!clickable(e)) return;
                onClick.accept(t.getColumnName(t.columnAtPoint(e.getPoint())), m.rows.get(t.rowAtPoint(e.getPoint())));
            }
        };
        t.addMouseListener(ma); t.addMouseMotionListener(ma);

        Surface c = card(new BorderLayout());
        c.setBorder(new EmptyBorder(4, 4, 8, 4));
        c.add(h, BorderLayout.NORTH); c.add(t, BorderLayout.CENTER);
        return c;
    }

    //FILTERBAR
    static final String[] CATS = {"All categories", "Fiction", "Literature", "Science", "Self-development"};
    static final String[] SORTS = {"Recently added", "Title A–Z", "Author A–Z"};

    class FilterBar extends VBox {
        final Field search = new Field("Search by title, author, or ISBN");
        final Label count = new Label("", 12, false, R.MUTED);
        final Btn[] tabs = new Btn[3];
        String cat = CATS[0], sort = SORTS[0], type = "All";

        FilterBar(Runnable onChange) {
            JPanel r1 = new JPanel(new GridBagLayout()); r1.setOpaque(false);
            GridBagConstraints g = new GridBagConstraints();
            g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.gridx = 0; g.insets = new Insets(0, 0, 0, 10);
            r1.add(search, g);
            g.weightx = 0; g.gridx = 1; r1.add(new Dropdown(CATS, s -> { cat = s; onChange.run(); }), g);
            g.gridx = 2; g.insets = new Insets(0, 0, 0, 0); r1.add(new Dropdown(SORTS, s -> { sort = s; onChange.run(); }), g);

            JPanel tabRow = new JPanel(); tabRow.setOpaque(false);
            tabRow.setLayout(new BoxLayout(tabRow, BoxLayout.X_AXIS));
            String[] names = {"All books", "Printed", "E-books"}, types = {"All", "Printed", "E-book"};
            for (int i = 0; i < 3; i++) {
                final int k = i;
                Btn b = new Btn(names[i], Btn.Kind.TAB);
                b.setBorder(new EmptyBorder(6, 12, 6, 12));
                b.addActionListener(e -> {
                    type = types[k];
                    for (int j = 0; j < 3; j++) tabs[j].active(j == k);
                    onChange.run();
                });
                tabs[i] = b; tabRow.add(b); tabRow.add(Box.createHorizontalStrut(6));
            }
            tabs[0].active(true);
            JPanel r2 = new JPanel(new BorderLayout()); r2.setOpaque(false);
            r2.add(tabRow, BorderLayout.WEST); r2.add(count, BorderLayout.EAST);

            put(r1).gap(12).put(r2);
            search.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { onChange.run(); }
                public void removeUpdate(DocumentEvent e) { onChange.run(); }
                public void changedUpdate(DocumentEvent e) { onChange.run(); }
            });
        }

        List<Book> apply() {
            String q = search.getText().trim().toLowerCase();
            List<Book> out = new ArrayList<>();
            for (Book b : lib.books) {
                if (!q.isEmpty() && !(b.title + " " + b.author + " " + b.isbn).toLowerCase().contains(q)) continue;
                if (!cat.startsWith("All") && !b.category.equals(cat)) continue;
                if (!type.equals("All") && !b.type.equals(type)) continue;
                out.add(b);
            }
            if (sort.startsWith("Title")) out.sort(Comparator.comparing((Book b) -> b.title));
            else if (sort.startsWith("Author")) out.sort(Comparator.comparing((Book b) -> b.author));
            else Collections.reverse(out);
            count.setText("Showing " + out.size() + " of " + lib.books.size() + " titles");
            return out;
        }
    }

    void borrowOrReturn(Book b) {
        if (currentUserId == null) return;   
        try {
            if (b.status().equals("Available")) Db.borrow(b.id, currentUserId);
            else if (b.mine) Db.returnBook(b.id, currentUserId);
            else return;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(root, "That didn't save to the database:\n" + ex.getMessage(),
                                           "Database error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        loadFromDb(); 
    }

    //SCREEN
    JComponent dashboardPage() {
        Tall p = new Tall();
        Runnable fill = () -> {
            p.removeAll();
            p.put(hero("Welcome Back, " + userName + "!", "Here is the overview for the DEIHS Library.", null)).gap(22);
            p.put(new Label("LIBRARY OVERVIEW", 10, true, R.MUTED)).gap(10);

            JPanel stats = new JPanel(new GridLayout(1, 5, 14, 0)); stats.setOpaque(false);
            int total = lib.total(), avail = lib.available();
            stats.add(stat("Total Books", fmt(total)));
            stats.add(stat("Available", fmt(avail)));
            stats.add(stat("Unavailable", fmt(total - avail)));
            stats.add(stat("Printed Books", fmt(lib.copiesOf("Printed"))));
            stats.add(stat("E-Books", fmt(lib.copiesOf("E-book"))));
            p.put(stats).gap(18);

            JPanel two = new JPanel(new GridBagLayout()); two.setOpaque(false);
            GridBagConstraints g = new GridBagConstraints();
            g.fill = GridBagConstraints.BOTH; g.weighty = 1; g.gridx = 0; g.weightx = 1.7; g.insets = new Insets(0, 0, 0, 14);
            two.add(recentCard(), g);
            g.gridx = 1; g.weightx = 1; g.insets = new Insets(0, 0, 0, 0);
            two.add(sideCards(total, avail), g);
            p.put(two);
            p.revalidate(); p.repaint();
        };
        lib.listeners.add(fill); fill.run();
        return scroll(p);
    }

    JComponent stat(String label, String value) {
        Surface c = card(new BorderLayout()); c.setBorder(new EmptyBorder(16, 16, 16, 16));
        c.add(new VBox().put(new Chip()).gap(12).put(new Label(label, 12, false, R.MUTED)).gap(2)
              .put(new Label(value, 26, true, R.TEXT)));
        return c;
    }

    JComponent recentCard() {
        Surface c = card(new BorderLayout()); c.setBorder(new EmptyBorder(18, 18, 8, 18));
        VBox v = new VBox();
        JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
        head.add(new Label("Recently added", 15, true, R.TEXT), BorderLayout.WEST);
        Btn more = new Btn("View collections ›", Btn.Kind.GHOST); more.setBorder(new EmptyBorder(0, 0, 0, 0));
        more.addActionListener(e -> go("Collections"));
        head.add(more, BorderLayout.EAST);
        v.put(head).gap(2).put(new Label("A few fresh picks from our shelves.", 12, false, R.MUTED)).gap(10);

        int shown = 0;
        for (int i = lib.books.size() - 1; i >= 0 && shown < 4; i--, shown++) {
            Book b = lib.books.get(i);
            Row r = new Row(); r.setPreferredSize(new Dimension(1, 62));
            GridBagConstraints g = new GridBagConstraints(); g.anchor = GridBagConstraints.WEST;
            Cover cv = new Cover(b, true); cv.setPreferredSize(new Dimension(30, 42));
            g.insets = new Insets(0, 0, 0, 12); r.add(cv, g);
            g.gridx = 1; g.weightx = 1; g.insets = new Insets(0, 0, 0, 12);
            r.add(new VBox().put(new Label(b.title, 13, true, R.TEXT)).put(new Label(b.author, 12, false, R.MUTED)), g);
            g.gridx = 2; g.weightx = 0; r.add(new Label(b.type, 12, false, R.MUTED), g);
            g.gridx = 3; g.insets = new Insets(0, 24, 0, 0); r.add(statusPill(b), g);
            if (shown == 3 || i == 0) r.line = false;
            v.put(r);
        }
        c.add(v, BorderLayout.NORTH);
        return c;
    }

    JComponent sideCards(int total, int avail) {
        JPanel col = new JPanel(new GridBagLayout()); col.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH; g.weightx = 1; g.weighty = 1; g.gridx = 0; g.gridy = 0;

        Surface ready = card(new BorderLayout()); ready.setBorder(new EmptyBorder(18, 18, 18, 18));
        int pct = total == 0 ? 0 : Math.round(100f * avail / total);
        JPanel big = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); big.setOpaque(false);
        big.add(new Label(pct + "%", 34, true, R.ACCENT)); big.add(new Label("of our collection is available", 12, false, R.MUTED));
        JPanel nums = new JPanel(new BorderLayout()); nums.setOpaque(false);
        nums.add(new Label(fmt(avail) + " available", 11, false, R.MUTED), BorderLayout.WEST);
        nums.add(new Label(fmt(total - avail) + " on loan", 11, false, R.MUTED), BorderLayout.EAST);
        ready.add(new VBox().put(new Label("Ready to borrow", 15, true, R.TEXT)).gap(8).put(big).gap(8)
                  .put(new Meter(total == 0 ? 0 : (double) avail / total)).gap(6).put(nums), BorderLayout.NORTH);
        g.insets = new Insets(0, 0, 14, 0); col.add(ready, g);

        Surface cta = card(new BorderLayout()); cta.setBorder(new EmptyBorder(18, 18, 18, 18));
        Btn browse = full(new Btn("Browse catalog", Btn.Kind.PRIMARY)); browse.addActionListener(e -> go("Catalog"));
        cta.add(new VBox().put(new Label("Your next chapter starts here.", 15, true, R.TEXT)).gap(4)
                .put(new Label("Find a title, check availability, and borrow.", 12, false, R.MUTED)).gap(12)
                .put(browse).gap(8).put(new Label("Browse " + fmt(total) + " books across all categories.", 11, false, R.MUTED)),
                BorderLayout.NORTH);
        g.gridy = 1; g.insets = new Insets(0, 0, 0, 0); col.add(cta, g);
        return col;
    }

    JComponent collectionsPage() {
        Tall p = new Tall();
        JPanel grid = new JPanel(new GridLayout(0, 4, 16, 16)); grid.setOpaque(false);
        int[] cols = {4};
        FilterBar[] fb = new FilterBar[1];
        Runnable refresh = () -> {
            grid.removeAll();
            List<Book> shown = fb[0].apply();
            for (Book b : shown) grid.add(bookCard(b));
            while (grid.getComponentCount() % cols[0] != 0) { JPanel e = new JPanel(); e.setOpaque(false); grid.add(e); }
            grid.revalidate(); grid.repaint();
        };
        fb[0] = new FilterBar(refresh);
        grid.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int n = Math.max(2, grid.getWidth() / 230);
                if (n != cols[0]) { cols[0] = n; ((GridLayout) grid.getLayout()).setColumns(n); refresh.run(); }
            }
        });
        p.put(hero("Find your next great read.", "Explore the DEIHS Library, one book at a time.", null)).gap(18)
         .put(fb[0]).gap(16).put(grid);
        lib.listeners.add(refresh); refresh.run();
        return scroll(p);
    }

    static JComponent bookCard(Book b) {
        Surface c = card(new BorderLayout(0, 10)); c.setBorder(new EmptyBorder(8, 8, 14, 8));
        Cover cv = new Cover(b, false); cv.setPreferredSize(new Dimension(150, 210));
        JPanel info = new JPanel(new BorderLayout()); info.setOpaque(false); info.setBorder(new EmptyBorder(0, 4, 0, 4));
        info.add(new VBox().put(new Label(b.title, 13, true, R.TEXT)).put(new Label(b.author, 12, false, R.MUTED))
                 .put(new Label(b.category, 11, false, R.MUTED)));
        c.add(cv, BorderLayout.CENTER); c.add(info, BorderLayout.SOUTH);
        return c;
    }

    JComponent catalogPage() {
        Tall p = new Tall();
        BookModel m = new BookModel("Title", "Catalog", "Type", "Status", "Action");
        FilterBar[] fb = new FilterBar[1];
        Runnable refresh = () -> { m.rows = fb[0].apply(); m.fireTableDataChanged(); };
        fb[0] = new FilterBar(refresh);
        p.put(hero("A world of knowledge, within reach.", "Search the catalog, check availability, and borrow your next book.", null)).gap(18)
         .put(fb[0]).gap(14)
         .put(tableCard(m, (col, b) -> { if (col.equals("Action")) borrowOrReturn(b); }));
        lib.listeners.add(refresh); refresh.run();
        return scroll(p);
    }

    JComponent managePage() {
        Tall p = new Tall();
        BookModel m = new BookModel("Title", "Catalog", "Type", "Copies", "Remove");
        FilterBar[] fb = new FilterBar[1];
        Runnable refresh = () -> { m.rows = fb[0].apply(); m.fireTableDataChanged(); };
        fb[0] = new FilterBar(refresh);

        Btn reload = new Btn("Refresh catalog", Btn.Kind.OUTLINE); reload.addActionListener(e -> loadFromDb());
        JPanel body = new JPanel(new BorderLayout(16, 0)); body.setOpaque(false);
        JComponent table = tableCard(m, (col, b) -> {
            if (!col.equals("Remove") || !isLibrarian()) return;
            try { Db.deleteBook(b.id); loadFromDb(); }
            catch (SQLException ex) {
                JOptionPane.showMessageDialog(root,
                    "Couldn't remove \"" + b.title + "\" -- it still has loan history on record:\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel tableHolder = new JPanel(new BorderLayout()); tableHolder.setOpaque(false);
        tableHolder.add(table, BorderLayout.NORTH);
        body.add(tableHolder, BorderLayout.CENTER); body.add(addForm(), BorderLayout.EAST);

        p.put(hero("A well-kept collection, a better library.", "Manage the inventory. Add new books, remove titles, and keep your catalog current.", reload)).gap(18)
         .put(fb[0]).gap(14).put(body);
        lib.listeners.add(refresh); refresh.run();
        return scroll(p);
    }

    JComponent addForm() {
        Surface c = card(new BorderLayout()); c.setBorder(new EmptyBorder(18, 18, 18, 18));
        c.setPreferredSize(new Dimension(310, 10));
        Field title = new Field("Enter book title"), author = new Field("Enter author name");
        Field copies = new Field("1"), isbn = new Field("e.g. 9780735211292");
        Dropdown cat = new Dropdown(new String[]{"Select category", "Fiction", "Literature", "Science", "Self-development"}, s -> {});
        Dropdown type = new Dropdown(new String[]{"Printed", "E-book"}, s -> {});
        Label err = new Label(" ", 12, false, R.DANGER);
        File[] picked = {null};
        Label coverName = new Label("No file chosen", 12, false, R.MUTED);
        Btn choose = new Btn("Choose file…", Btn.Kind.OUTLINE); choose.setBorder(new EmptyBorder(8, 14, 8, 14));
        choose.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(c);
            FileDialog fd = new FileDialog(w instanceof Frame ? (Frame) w : null, "Choose a cover image", FileDialog.LOAD);
            fd.setFilenameFilter((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));  
            fd.setVisible(true);                       
            if (fd.getFile() == null) return;          
            File f = new File(fd.getDirectory(), fd.getFile());
            try { if (ImageIO.read(f) == null) throw new java.io.IOException(); }
            catch (Exception ex) { err.setText("That file isn't a readable image."); return; }
            picked[0] = f; coverName.setText(f.getName()); err.setText(" ");
        });
        JPanel coverRow = new JPanel(new BorderLayout(10, 0)); coverRow.setOpaque(false);
        coverRow.add(choose, BorderLayout.WEST); coverRow.add(coverName, BorderLayout.CENTER);
        Btn add = new Btn("Add book", Btn.Kind.PRIMARY); add.setPreferredSize(new Dimension(100, 42)); full(add);

        JPanel pair = new JPanel(new GridLayout(1, 2, 8, 0)); pair.setOpaque(false);
        pair.add(new VBox().put(fieldLabel("Type")).gap(4).put(full(type)));
        pair.add(new VBox().put(fieldLabel("Copies")).gap(4).put(full(copies)));
        pair.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        VBox v = new VBox();
        v.put(new Label("Add a new book", 15, true, R.TEXT)).gap(2)
         .put(new Label("Grow the library, one title at a time.", 12, false, R.MUTED)).gap(14)
         .put(fieldLabel("Book title")).gap(4).put(title).gap(10)
         .put(fieldLabel("Author")).gap(4).put(author).gap(10)
         .put(fieldLabel("Catalog / category")).gap(4).put(full(cat)).gap(10)
         .put(pair).gap(10)
         .put(fieldLabel("ISBN")).gap(4).put(isbn).gap(10)
         .put(fieldLabel("Cover image (optional)")).gap(4).put(coverRow).gap(6).put(err).gap(6).put(add);

        add.addActionListener(e -> {
            if (!isLibrarian()) return;
            if (title.getText().trim().isEmpty() || author.getText().trim().isEmpty()) { err.setText("Add a title and an author."); return; }
            if (cat.value.startsWith("Select")) { err.setText("Pick a category."); return; }
            int n; try { n = Integer.parseInt(copies.getText().trim()); } catch (NumberFormatException ex) { n = 1; }
            n = Math.max(1, n);
            String coverPath = null;
            if (picked[0] != null) {
                try { coverPath = saveCover(picked[0], title.getText().trim()); }
                catch (java.io.IOException ex) { err.setText("Couldn't copy the cover image."); return; }
            }
            Book nb = new Book(title.getText().trim(), author.getText().trim(), cat.value, type.value, n, n,
                               Db.hueOf(title.getText().trim()), false, isbn.getText().trim());
            nb.coverPath = coverPath;          
            try { Db.insertBook(nb); }
            catch (SQLException ex) { err.setText("Couldn't save that book to the database: " + ex.getMessage()); return; }
            title.setText(""); author.setText(""); copies.setText(""); isbn.setText(""); cat.set("Select category");
            picked[0] = null; coverName.setText("No file chosen");
            err.setText(" "); loadFromDb();
        });
        c.add(v, BorderLayout.NORTH);
        return c;
    }

    static <T extends JComponent> T indent(T c) { c.setBorder(new EmptyBorder(0, 12, 0, 0)); return c; }

    static Label fieldLabel(String s) { return new Label(s, 12, true, R.TEXT); }

    //LOGINSIDEBAR
    final CardLayout screens = new CardLayout();
    final Surface root = new Surface(screens, R.BG, null, 0);
    final CardLayout pages = new CardLayout();
    final JPanel pageHost = new JPanel(pages);
    final Map<String, Btn> nav = new LinkedHashMap<>();
    final List<Runnable> themeSyncers = new ArrayList<>();
    final Label topTitle = new Label("Dashboard", 14, true, R.TEXT);

    public LibraryUI() {
        root.add(loginScreen(), "login");
        root.add(appScreen(), "app");
        screens.show(root, "login");
    }

    String userName = "User", role = "student";
    Integer currentUserId = null;
    boolean isLibrarian() { return "librarian".equals(role); }


    String[] authenticate(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) return null;
        try {
            return Db.authenticate(username, password);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(root, "Couldn't reach the database:\n" + ex.getMessage(),
                                           "Database error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    void signIn(int userId, String name, String r) {
        currentUserId = userId; userName = name; role = r;
        nav.get("Manage Collections").setVisible(isLibrarian());   
        loadFromDb();                                               
        screens.show(root, "app");
        go("Dashboard");
    }

    void signOut() {
        currentUserId = null;
        userName = "User"; role = "student";
        screens.show(root, "login");
    }

    void go(String key) {
        if (key.equals("Manage Collections") && !isLibrarian()) key = "Dashboard";   
        pages.show(pageHost, key);
        final String shown = key;
        nav.forEach((k, b) -> b.active(k.equals(shown)));
        topTitle.setText(key);
    }

    void setTheme(Theme t) {
        Theme.current = t;
        themeSyncers.forEach(Runnable::run);
        root.repaint();
    }

    JComponent themeToggle() {
        Surface s = new Surface(new FlowLayout(FlowLayout.LEFT, 2, 2), R.SURFACE, R.BORDER, 8);
        Btn light = new Btn("Light", Btn.Kind.TAB), dark = new Btn("Dark", Btn.Kind.TAB);
        for (Btn b : new Btn[]{light, dark}) b.setBorder(new EmptyBorder(5, 12, 5, 12));
        light.addActionListener(e -> setTheme(Theme.LIGHT));
        dark.addActionListener(e -> setTheme(Theme.DARK));
        Runnable sync = () -> { light.active(!Theme.dark()); dark.active(Theme.dark()); };
        themeSyncers.add(sync); sync.run();
        s.add(light); s.add(dark);
        return s;
    }

    JComponent loginScreen() {
        Surface page = new Surface(new BorderLayout(), R.BG, null, 0);
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false); top.setBorder(new EmptyBorder(16, 24, 0, 24));
        top.add(new Label("DEIHS / LIBRARY", 11, false, R.MUTED), BorderLayout.WEST);
        top.add(themeToggle(), BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);

        Surface card = card(new BorderLayout()); card.setBorder(new EmptyBorder(28, 28, 24, 28));
        card.setPreferredSize(new Dimension(380, 495));
        Field user = new Field("Enter your username"); PField pass = new PField("Enter your password");
        Btn sign = new Btn("Sign in", Btn.Kind.PRIMARY); sign.setPreferredSize(new Dimension(100, 42)); full(sign);
        Label loginErr = new Label(" ", 12, false, R.DANGER);
        sign.addActionListener(e -> {
            String u = user.getText().trim(); String pw = new String(pass.getPassword());
            if (u.isEmpty() || pw.isEmpty()) { loginErr.setText("Enter your username and password."); return; }
            String[] who = authenticate(u, pw);
            if (who == null) { loginErr.setText("Incorrect username or password."); return; }
            loginErr.setText(" "); user.setText(""); pass.setText("");
            signIn(Integer.parseInt(who[0]), who[1], who[2]);
        });
        pass.addActionListener(e -> sign.doClick());
        Btn forgot = new Btn("Forgot password?", Btn.Kind.GHOST); forgot.setBorder(new EmptyBorder(0, 0, 0, 0));
        JPanel opts = new JPanel(new BorderLayout()); opts.setOpaque(false);
        opts.add(new Check("Remember me"), BorderLayout.WEST); opts.add(forgot, BorderLayout.EAST);
        opts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        VBox v = new VBox();
        VBox head = new VBox(true);
        head.put(new Logo(48)).gap(8).put(new Label("DEIHS Library", 14, true, R.TEXT));
        JPanel headWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); headWrap.setOpaque(false); headWrap.add(head);
        v.put(headWrap).gap(18)
         .put(new Label("Welcome back.", 22, true, R.TEXT)).gap(2)
         .put(new Label("Sign in to explore your library.", 13, false, R.MUTED)).gap(18)
         .put(fieldLabel("Username")).gap(4).put(user).gap(12)
         .put(fieldLabel("Password")).gap(4).put(pass).gap(10).put(opts).gap(6).put(loginErr).gap(6).put(sign).gap(14)
         .put(new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)) {{ setOpaque(false); add(new Label("Need access? Contact your librarian", 12, false, R.MUTED)); }});
        card.add(v, BorderLayout.NORTH);

        VBox center = new VBox(true);
        center.put(card).gap(18).put(new Label("Dasmariñas East Integrated High School", 11, false, R.MUTED));
        JPanel mid = new JPanel(new GridBagLayout()); mid.setOpaque(false); mid.add(center);
        page.add(mid, BorderLayout.CENTER);
        return page;
    }

    JComponent appScreen() {
        Surface app = new Surface(new BorderLayout(), R.BG, null, 0);

        Surface side = new Surface(new BorderLayout(), R.SURFACE, null, 0).edge('r');
        side.setPreferredSize(new Dimension(236, 10)); side.setBorder(new EmptyBorder(20, 14, 20, 14));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(0, 0, 0, 0)); brand.add(new Logo(34));
        brand.add(new VBox().put(new Label("DEIHS", 15, true, R.TEXT)).put(new Label("LIBRARY SYSTEM", 9, false, R.MUTED)));
        VBox menu = new VBox();
        menu.put(brand).gap(22).put(indent(new Label("WORKSPACE", 10, true, R.MUTED))).gap(8);
        for (String k : new String[]{"Dashboard", "Collections", "Catalog", "Manage Collections"}) {
            Btn b = full(new Btn(k, Btn.Kind.NAV)); b.addActionListener(e -> go(k));
            nav.put(k, b); menu.put(b).gap(4);
        }
        side.add(menu, BorderLayout.NORTH);
        side.add(new VBox().put(indent(new Label("DEIHS Library", 12, true, R.TEXT)))
                 .put(indent(new Label("A space for every curious mind.", 11, false, R.MUTED))), BorderLayout.SOUTH);

        Surface bar = new Surface(new BorderLayout(), R.BG, null, 0).edge('b');
        bar.setBorder(new EmptyBorder(14, 20, 14, 20));
        topTitle.setHorizontalAlignment(SwingConstants.CENTER);
        Btn out = new Btn("Sign out", Btn.Kind.OUTLINE); out.addActionListener(e -> signOut());
        bar.add(themeToggle(), BorderLayout.WEST); bar.add(topTitle, BorderLayout.CENTER); bar.add(out, BorderLayout.EAST);

        pageHost.setOpaque(false);
        pageHost.add(dashboardPage(), "Dashboard");
        pageHost.add(collectionsPage(), "Collections");
        pageHost.add(catalogPage(), "Catalog");
        pageHost.add(managePage(), "Manage Collections");
        go("Dashboard");

        JPanel right = new JPanel(new BorderLayout()); right.setOpaque(false);
        right.add(bar, BorderLayout.NORTH); right.add(pageHost, BorderLayout.CENTER);
        app.add(side, BorderLayout.WEST); app.add(right, BorderLayout.CENTER);
        return app;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) { }
            JFrame f = new JFrame("DEIHS Library Management System");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new LibraryUI().root);
            f.setMinimumSize(new Dimension(1000, 680));
            f.setSize(1280, 800);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
