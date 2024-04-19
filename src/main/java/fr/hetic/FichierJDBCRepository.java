package fr.hetic;

import fr.hetic.entities.*;
import java.io.*;
import java.sql.*;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

class SyntaxErrorException extends Exception {
    public SyntaxErrorException(String message) {
        super(message);
    }
}

// Classe d'exception métier pour les opérations non supportées
class UnsupportedOperatorException extends Exception {
    public UnsupportedOperatorException(String message) {
        super(message);
    }
}

// Classe d'exception métier pour les erreurs lors de l'opération
class OperationExecutionException extends Exception {
    public OperationExecutionException(String message) {
        super(message);
    }
}

class LineProcessingException extends Exception {
    public LineProcessingException(String message) {
        super(message);
    }
}

public class FichierJDBCRepository {
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Value("${db.url}")
    private String url;

    @Value("${db.user}")
    private String user;

    @Value("${db.password}")
    private String password;

    @Autowired
    public FichierJDBCRepository(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    public void getFileFromDb() throws LineProcessingException{
        String url = "jdbc:postgresql://SG-hetic-mt4-java-5275-pgsql-master.servers.mongodirector.com:5432/TP";
        String user = "etudiant";
        String password = "MT4@hetic2324";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String sqlFichier = "SELECT * FROM FICHIER WHERE TYPE = 'OP'";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlFichier)) {

                while (rs.next()) {
                    FileEntity file = new FileEntity();
                    file.setId(rs.getInt("ID"));
                    file.setNom(rs.getString("NOM"));
                    file.setType(rs.getString("TYPE"));

                    processFileFromDb(file, conn);
                }
            }
        } catch (SQLException e) {
           
        }
    }
    public void processFileFromDb(FileEntity fileEntity, Connection conn) throws LineProcessingException{
        String outputFilePath = fileEntity.getNom() + ".res";
    File outputFile = new File(outputFilePath);

    String sqlLigne = "SELECT * FROM LIGNE WHERE FICHIER_ID = " + fileEntity.getId();

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sqlLigne);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            while (rs.next()) {
                LineEntity line = new LineEntity();
                line.setId(rs.getInt("ID"));
                line.setParam1(rs.getInt("PARAM1"));
                line.setParam2(rs.getInt("PARAM2"));
                line.setOperateur(rs.getString("OPERATEUR").charAt(0));
                line.setFichierId(rs.getInt("FICHIER_ID"));

                try {
                   String lineString = line.getParam1() + " " + line.getParam2() + " " + line.getOperateur();
                   String result = processLine(lineString);
                   writer.write(result);
                   writer.newLine();
                } catch (Exception e) {
                    writer.write(e.getMessage());
                    writer.newLine();
                }
            }
        } catch (SQLException | IOException e) {
            System.out.println("An error occurred processing file " + fileEntity.getNom() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
   
    public static String processLine (String line) throws LineProcessingException {
        try {
            String[] parts = line.split(" ");
            checkSyntax(line);
            
                Integer firstNumber = Integer.parseInt(parts[0]);
                Integer secondNumber = Integer.parseInt(parts[1]);
                String operation = parts[2];
                Integer result = calculate(firstNumber, secondNumber, operation);
                if (result != null) {
                    return firstNumber + " " + operation + " " + secondNumber + " = " + result;
                    // writer.write(firstNumber + " " + operation + " " + secondNumber + " = " + result);
                    // writer.newLine();
                } else {
                    throw new UnsupportedOperatorException(operation + " is not a valid operator");
                }
        } catch (Exception e) {
           throw new LineProcessingException(e.getMessage());
        }
    }

    private static void checkSyntax(String line) throws SyntaxErrorException {
        String[] parts = line.split(" ");
        if (parts.length != 3) {
            throw new SyntaxErrorException(
                "Invalid number of arguments expected 3 but found " + parts.length + " in line : " + line);
        }
        if (!parts[0].matches("-?\\d+") || !parts[1].matches("-?\\d+")) {
            throw new SyntaxErrorException("Invalid syntax in line : " + line
                + " Expected <Integer> <Integer> <String> but found : " + parts[0] + " " + parts[1] + " " + parts[2]);
        }
    }

    private static Integer calculate(int a, int b, String operation) throws UnsupportedOperatorException{
        OperationFactory factory = new OperationFactory();
        Expression expression = factory.createExpression(operation);
        if(expression == null){
            throw new UnsupportedOperatorException(operation + " is not a valid operator");
        }
        return expression.evaluate(a, b);
    }
}
