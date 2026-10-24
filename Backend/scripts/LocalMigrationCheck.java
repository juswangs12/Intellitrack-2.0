import java.nio.file.*;
import java.sql.*;
import java.util.*;
/** Manual local-only regression check. Creates and removes its own temporary database. */
public class LocalMigrationCheck {
  public static void main(String[] args) throws Exception {
    Map<String,String> env=new HashMap<>();
    for(String line:Files.readAllLines(Path.of(".env"))){int i=line.indexOf('=');if(i>0&&!line.strip().startsWith("#")){String v=line.substring(i+1).strip();if(v.length()>1&&((v.startsWith("\"")&&v.endsWith("\""))||(v.startsWith("'")&&v.endsWith("'"))))v=v.substring(1,v.length()-1);env.put(line.substring(0,i).strip(),v);}}
    String url=env.get("MYSQL_URL");
    if(url==null||!(url.startsWith("jdbc:mysql://localhost:")||url.startsWith("jdbc:mysql://127.0.0.1:")))throw new IllegalStateException("Only an explicitly local MySQL URL is allowed");
    String db="intellitrack_check_"+UUID.randomUUID().toString().replace("-", "");
    String sql=Files.readString(Path.of("src/main/resources/schema-migration.sql")).replaceAll("(?m)^--.*$", "");
    try(Connection c=DriverManager.getConnection(url,env.getOrDefault("MYSQL_USER","root"),env.getOrDefault("MYSQL_PASSWORD",""));Statement s=c.createStatement()){
      s.execute("CREATE DATABASE `"+db+"`");
      try {
        s.execute("USE `"+db+"`");
        s.execute("CREATE TABLE submissions(id bigint PRIMARY KEY, status varchar(20), revision_count int)");
        s.execute("INSERT INTO submissions VALUES(1,'PENDING',0)");
        s.execute("CREATE TABLE class_sections(id bigint PRIMARY KEY)");
        s.execute("INSERT INTO class_sections VALUES(1)");
        s.execute("CREATE TABLE deliverables(id bigint PRIMARY KEY,name varchar(255),subject_id bigint,academic_term varchar(255),class_section_id bigint,UNIQUE KEY old_subject(name,subject_id),UNIQUE KEY old_section(class_section_id,name),CONSTRAINT section_fk FOREIGN KEY(class_section_id) REFERENCES class_sections(id))");
        s.execute("INSERT INTO deliverables VALUES(1,'Proposal',1,'2026-2027 / 1',1)");
        for(int pass=0;pass<2;pass++)for(String statement:sql.split(";"))if(!statement.isBlank())s.execute(statement);
        s.execute("INSERT INTO deliverables VALUES(2,'Proposal',1,'2026-2027 / 2',1)");
        boolean rejected=false;
        try{s.execute("INSERT INTO deliverables VALUES(3,'Proposal',1,'2026-2027 / 2',1)");}catch(SQLIntegrityConstraintViolationException expected){rejected=true;}
        if(!rejected)throw new AssertionError("Duplicate subject/term requirement accepted");
        try(ResultSet r=s.executeQuery("SELECT COUNT(*) FROM deliverables")){r.next();if(r.getInt(1)!=2)throw new AssertionError("Rows were not preserved");}
        System.out.println("PASS: MySQL migration runs twice, preserves rows and foreign keys, allows separate terms and rejects same-term duplicates.");
      } finally {s.execute("DROP DATABASE `"+db+"`");System.out.println("Temporary validation database removed.");}
    }
    String key=env.get("GOOGLE_TOKEN_ENCRYPTION_KEY");
    if(key==null||Base64.getDecoder().decode(key).length!=32)throw new AssertionError("Invalid encryption key");
    System.out.println("PASS: local encryption key decodes to 32 bytes (value not displayed).");
  }
}
