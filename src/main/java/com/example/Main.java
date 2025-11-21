package com.example;
import java.sql.*;
import com.mysql.cj.jdbc.Driver;
import java.util.Scanner;
import java.util.concurrent.LinkedTransferQueue;

public class Main {
    protected static Scanner sc = new Scanner(System.in);
    private static final String url = "jdbc:mysql://localhost:3306/homework2";
    private static final String username = "root"; // исправлено
    private static final String password = "MyNewPass123!";
    protected static Connection connection;
    private static String userName; // для хранения имени
    private static String tablename;

    public static void main(String[] args)  {

        System.out.println("Введите ваше имя: ");
        userName = sc.nextLine();

        try {
            getConnection();
            System.out.println("✓ Подключение к базе данных установлено!\n");
        } catch (SQLException e) {
            System.err.println("✗ Ошибка подключения к БД: " + e.getMessage());
            return; // Выход из программы
        }

        boolean running = true;
        while (running) {
            displayMenu();
            int numberMenu = getMenuChoice();
            switch (numberMenu) {
                case 1 -> showTables();
                case 2 -> makeTable();
                case 3 -> checkNumbers();
                case 4 -> exportToExcel();
                case 0 -> {
                    System.out.println("Выход из программы. До свидания!");
                    running = false;
                }
                default -> System.out.println("Неверный выбор! Попробуйте снова.");
            }
        }
    }

    protected static void displayMenu() {
        // Используем сохраненное имя
        System.out.println("Добро пожаловать в программу, " + userName + "!");
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║      ИНТЕРАКТИВНОЕ КОНСОЛЬНОЕ МЕНЮ         ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.println("║ 1. Вывести все таблицы из MySQL            ║");
        System.out.println("║ 2. Создать таблицу в MySQL                 ║");
        System.out.println("║ 3. Проверка чисел (базовый вариант)        ║");
        System.out.println("║ 4. Экспорт данных в Excel                  ║");
        System.out.println("║ 0. Выход                                   ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.print("Выберите пункт меню: ");
    }

    private static int getMenuChoice() {
        String numb = sc.nextLine();
        try {
            return Integer.parseInt(numb);
        } catch (NumberFormatException e) {
            System.out.println("Неверный формат ввода");
            return -1;
        }
    }
    private static void getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            // Регистрируем драйвер
            DriverManager.registerDriver(new Driver());
            // Создаем подключение
            connection = DriverManager.getConnection(url, username, password);
        }
    }
    private static void showTables() {
        System.out.println("Вы выбрали действие 1 - вывести все таблицы из MySQL");
        boolean hasTable = false;
        try (Statement stmt = connection.createStatement();
             ResultSet res = stmt.executeQuery("SHOW TABLES")) {
            while (res.next()) {
                System.out.println(res.getString(1)); // ВАЖНО: добавить индекс 1
                hasTable = true;
            }
            if (!hasTable) {
                System.out.println("В БД пока что нет таблиц");
            }
        } catch (SQLException e) {
            System.err.println("✗ Ошибка при получении таблиц: " + e.getMessage());
        }
        System.out.println();
    }


    private static void makeTable() {
        System.out.println("Вы выбрали действие 2 - создать таблицу в MySQL");
        try (Statement stmt1 = connection.createStatement()) {
            System.out.println("ВВедите название будущей таблицы: ");
            tablename = sc.nextLine();
            String query = "CREATE TABLE IF NOT EXISTS " + tablename
                    + " (id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "number INT, "
                    + "countUncount VARCHAR(255), "
                    + "mistake VARCHAR(255))"; // название ошибки
            stmt1.executeUpdate(query);
            System.out.println("Таблица" + tablename+ " создана или уже существует");
        } catch (SQLException e) {
            System.err.println("✗ Ошибка при создании таблицы: " + e.getMessage());
        }
        System.out.println();
    }

    private static void checkNumbers() {
        System.out.println("Вы выбрали действие 3 - проверка чисел");
        if (tablename == null) {
            System.out.println("Сначала создайте таблицу (пункт 2).");
            return;
        }

        System.out.println("Введите числа. Для выхода введите 'stop'.");

        while (true) {
            System.out.print("Введите число: ");
            String input = sc.nextLine();

            if (input.equalsIgnoreCase("stop")) break;

            int num;
            String error = null;
            String result = null;

            try {
                // Проверка целостности
                if (input.contains(",")) {
                    error = "Использована запятая";
                    throw new NumberFormatException();
                }

                num = Integer.parseInt(input);

                // Проверка четности
                result = (num % 2 == 0) ? "Чётное" : "Нечётное";

            } catch (NumberFormatException e) {
                num = 0;
                result = "Ошибка";
                error = "Введено не целое число";
                System.out.println("Ошибка: ввод должен быть целым числом.");
            }

            try {
                String insertQuery = "INSERT INTO " + tablename + " (number, countUncount, mistake) VALUES (?, ?, ?)";
                PreparedStatement ps = connection.prepareStatement(insertQuery);
                ps.setInt(1, num);
                ps.setString(2, result);
                ps.setString(3, error);

                ps.executeUpdate();

            } catch (SQLException ex) {
                System.err.println("Ошибка записи в БД: " + ex.getMessage());
            }
            if (error == null)
                System.out.println("Результат: число " + input + " → " + result);
        }

    }


    private static void exportToExcel() {
        System.out.println("Вы выбрали действие 4 - экспорт в Excel");
        if (tablename == null) {
            System.out.println("Сначала создайте таблицу (пункт 2).");
            return;
        }
        try (Statement stmt3 = connection.createStatement()) {
            System.out.println("Введите будущее название файла: ");
            String name = sc.nextLine();
            if (name.isEmpty()) {
                System.out.println("Неправильное имя файла");
                return;
            }
            String q = "SELECT * FROM " + tablename + " INTO OUTFILE 'D:/" + name + "' CHARACTER SET CP1251";
            Statement stmt7 = connection.createStatement();
            try {
                stmt7.executeQuery(q);
                System.out.println("Данные успешно экспортированы в файл!");
            } catch (SQLException e) {
                System.out.println("Ошибка экспорта: " + e.getMessage()); }
            String query1 = "SELECT * FROM " + tablename;
            PreparedStatement ps = connection.prepareStatement(query1);
            ResultSet res4 = ps.executeQuery();

            System.out.println("Все данные из таблицы:");
            while (res4.next()) {
                System.out.println("ID\tчисло\tЧётное или нечётное\tОшибка");
                System.out.println(
                        "ID" + res4.getString("id") + "\t"
                                + res4.getInt("number") + "\t"
                                 + res4.getString("countUncount") + "\t"
                                 + res4.getString("mistake"));
            }


        }
        catch (SQLException e) {
            System.err.println("✗ Ошибка при создании таблицы: " + e.getMessage());
        }
    }
}