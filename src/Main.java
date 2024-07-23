import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class Main {
    private Connection conn;
    private JFrame frame;
    private JComboBox<String> comboBox;
    private JTable table;
    private DefaultTableModel tableModel;
    private JProgressBar progressBar;

    public Main() {
        // Establecer conexión a la base de datos PostgreSQL
        connectDB();

        // Crear la interfaz gráfica
        createGUI();
    }

    private void connectDB() {
        try {
            String url = "jdbc:postgresql://localhost:5432/prueba2";
            String user = "postgres";
            String password = "elmejorcr7";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Conexión establecida con PostgreSQL.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        frame = new JFrame("Constructores con más Puntos Ganados");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        // Panel principal con borde y layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Encabezado personalizado
        JLabel headerLabel = new JLabel("Constructores con más Puntos Ganados", JLabel.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(new Color(0, 102, 204)); // Azul oscuro
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Panel para el combo box y la barra de progreso
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        comboBox = new JComboBox<>();
        populateComboBox();
        comboBox.addActionListener(e -> {
            // Cuando se seleccione un año, actualizar la tabla de constructores y puntos
            updateTableInBackground();
        });
        comboBox.setBackground(new Color(173, 216, 230)); // Azul claro
        comboBox.setForeground(Color.BLACK);
        comboBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        topPanel.add(comboBox, BorderLayout.WEST);
        progressBar = new JProgressBar();
        progressBar.setForeground(new Color(0, 102, 204)); // Azul oscuro
        topPanel.add(progressBar, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Tabla para mostrar los datos de constructores y puntos
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // Centrar el contenido de las celdas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);

        // Establecer el color de fondo de las celdas de la tabla
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(new Color(173, 216, 230)); // Azul claro
                setForeground(Color.BLACK);
                if (isSelected) {
                    setBackground(new Color(102, 178, 255)); // Azul de selección
                }
                return this;
            }
        });

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        frame.getContentPane().add(mainPanel);
        frame.setVisible(true);
    }

    private void populateComboBox() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT year FROM races ORDER BY year DESC");
            while (rs.next()) {
                comboBox.addItem(rs.getString("year"));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTableInBackground() {
        String selectedYear = (String) comboBox.getSelectedItem();
        if (selectedYear != null) {
            // Crear un SwingWorker para ejecutar la consulta en segundo plano
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        // Consulta para obtener los constructores y sus puntos totales para el año seleccionado
                        String query = "SELECT c.name AS constructor_name, SUM(cs.points) AS total_points " +
                                "FROM constructors c " +
                                "JOIN constructor_standings cs ON c.constructor_id = cs.constructor_id " +
                                "JOIN races r ON cs.race_id = r.race_id " +
                                "WHERE r.year = ? " +
                                "GROUP BY constructor_name " +
                                "ORDER BY total_points DESC";

                        PreparedStatement pstmt = conn.prepareStatement(query);
                        pstmt.setInt(1, Integer.parseInt(selectedYear));
                        ResultSet rs = pstmt.executeQuery();

                        // Obtener columnas
                        Vector<String> columnNames = new Vector<>();
                        columnNames.add("CONSTRUCTOR");
                        columnNames.add("TOTAL DE PUNTOS");

                        // Obtener filas
                        Vector<Vector<Object>> data = new Vector<>();
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            row.add(rs.getString("constructor_name"));
                            row.add(rs.getDouble("total_points"));
                            data.add(row);
                        }

                        // Actualizar modelo de la tabla en el hilo de eventos de Swing
                        SwingUtilities.invokeLater(() -> {
                            tableModel.setDataVector(data, columnNames);
                            progressBar.setValue(100); // Completa la barra de progreso
                        });

                        rs.close();
                        pstmt.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void done() {
                    // Aquí puedes realizar acciones adicionales después de que se completa la carga de datos
                    progressBar.setIndeterminate(false); // Detener el estado indeterminado
                }
            };

            // Iniciar el SwingWorker y mostrar la barra de progreso
            progressBar.setValue(0); // Reiniciar la barra de progreso
            progressBar.setIndeterminate(true); // Mostrar una barra de progreso indeterminada
            worker.execute();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
