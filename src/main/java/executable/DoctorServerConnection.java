
package executable;

import bitalino.BITalino;
import bitalino.BITalinoException;
import bitalino.BitalinoManager;
import bitalino.Frame;
import common.enums.Sex;
import pojos.DiagnosisFile;
import pojos.Patient;

import java.io.*;
import java.lang.reflect.Method;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.sql.Date;

public class DoctorServerConnection {

    //
    private static boolean isValidIPAddress(String ip) {
        if (ip.equalsIgnoreCase("localhost")) {
            return true;
        } else {
            String[] octets = ip.split("\\.");
            if (octets.length != 4) return false;
            for (String octet : octets) {
                try {
                    int value = Integer.parseInt(octet);
                    if (value < 0 || value > 255) return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
    }

    //
    private static String normalizeDNI(String dni) {
        if (dni == null) return null;
        return dni.replaceAll("[\\s-]", "").toUpperCase();
    }

    private static boolean validateDNI(String dni) {
        if (dni == null) return false;
        return dni.matches("\\d{8}[A-Z]");
    }

    private static boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.matches(emailRegex, email);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String serverAddress;
        final int port = 9000; // puerto fijo
        Socket socket = null;
        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;


        while (true) {
            System.out.println("Enter the IP address (or 'localhost'): ");
            serverAddress = scanner.nextLine();
            if (isValidIPAddress(serverAddress)) break;
            System.out.println("Invalid IP address format. Please try again.");
        }

        try {
            socket = new Socket(serverAddress, port);
            outputStream = new DataOutputStream(socket.getOutputStream());
            inputStream = new DataInputStream(socket.getInputStream());
            System.out.println("Connected to " + serverAddress + " at port " + port);

            // Identificar rol
            outputStream.writeUTF("Doctor");
            outputStream.flush();

            boolean loggedIn = false;
            String username = null;

            while (!loggedIn) {
                System.out.println("Choose: 1) Sign up   2) Log in   (type 1 or 2)");
                String choice = scanner.nextLine().trim();
                if ("1".equals(choice)) {
                    performSignUp(scanner, outputStream, inputStream);
                } else if ("2".equals(choice)) {
                    username = performLogin(scanner, outputStream, inputStream);
                    if (username != null) {
                        loggedIn = true;
                        System.out.println("Logged in as: " + username);
                    } else {
                        System.out.println("Login failed. Try again or sign up.");
                    }
                } else {
                    System.out.println("Invalid option. Type 1 or 2.");
                }
            }



        } catch (Throwable e) {
            Logger.getLogger(DoctorServerConnection.class.getName()).log(Level.SEVERE, "Error in the client", e);
        } finally {
            releaseResources(socket, outputStream, scanner, inputStream);
        }
    }


    // SIGN UP
    private static void performSignUp(Scanner scanner, DataOutputStream out, DataInputStream in) {
        try {
            System.out.println("---- SIGN UP (Doctor) ----");

            String username;
            while (true) {
                System.out.print("Username: ");
                username = scanner.nextLine().trim();
                if (!username.isEmpty()) break;
                System.out.println("Username cannot be empty.");
            }

            String password;
            while (true) {
                System.out.print("Password: ");
                password = scanner.nextLine();
                if (password.length() >= 6) break;
                System.out.println("Password must be at least 6 characters.");
            }

            String name;
            while (true) {
                System.out.print("Name: ");
                name = scanner.nextLine().trim();
                if (!name.isEmpty() && name.matches("[a-zA-Z ]+")) break;
                System.out.println("Invalid name. Only letters and spaces.");
            }

            String surname;
            while (true) {
                System.out.print("Surname: ");
                surname = scanner.nextLine().trim();
                if (!surname.isEmpty() && surname.matches("[a-zA-Z ]+")) break;
                System.out.println("Invalid surname. Only letters and spaces.");
            }

            String birthday;
            while (true) {
                System.out.print("Birthday (dd-MM-yyyy): ");
                birthday = scanner.nextLine().trim();
                if (birthday.matches("\\d{2}-\\d{2}-\\d{4}")) break;
                System.out.println("Invalid format. Use dd-MM-yyyy.");
            }

            String sex;
            Sex sexVal;
            while (true) {
                System.out.println("Please, type your sex (MALE/FEMALE):");
                sex = scanner.nextLine().trim();
                if (sex.equalsIgnoreCase("F") || sex.equalsIgnoreCase("Female")) {
                    sexVal = Sex.FEMALE;
                    break;
                } else if (sex.equalsIgnoreCase("M") || sex.equalsIgnoreCase("Male")) {
                    sexVal = Sex.MALE;
                    break;
                } else {
                    System.err.println("Invalid Sex, please select as shown");
                }
            }

            String email;
            while (true) {
                System.out.print("Email: ");
                email = scanner.nextLine().trim();
                if (isValidEmail(email)) break;
                System.out.println("Invalid email. Try again.");
            }

            // Campos que el servidor ESPERA para doctores
            System.out.print("Specialty: ");
            String specialty = scanner.nextLine().trim();

            System.out.print("License number: ");
            String licenseNumber = scanner.nextLine().trim();

            String dni;
            while (true) {
                System.out.print("DNI (8 digits + uppercase letter, e.g. 12345678Z) or NIE (X/Y/Z): ");
                String raw = scanner.nextLine().trim();
                dni = normalizeDNI(raw);
                if (validateDNI(dni)) break;
                System.out.println("Invalid DNI. Check the control letter and try again.");
            }


            // username, password, name, surname, birthday, sex, email, specialty, licenseNumber, dni
            out.writeUTF("SIGNUP");
            out.writeUTF(username);
            out.writeUTF(password);
            out.writeUTF(name);
            out.writeUTF(surname);
            out.writeUTF(birthday);
            out.writeUTF(String.valueOf(sexVal));
            out.writeUTF(email);
            out.writeUTF(specialty);
            out.writeUTF(licenseNumber);
            out.writeUTF(dni);
            out.flush();

            String response = in.readUTF();
            if ("ACK".equals(response)) {
                String msg = in.readUTF();
                System.out.println("Server: " + msg);
            } else if ("ERROR".equals(response)) {
                String msg = in.readUTF();
                System.err.println("Server error: " + msg);
            } else {
                System.out.println("Server (unexpected): " + response);
            }

        } catch (IOException e) {
            System.err.println("Error during sign-up: " + e.getMessage());
        }
    }

    // ===== LOGIN =====
    private static String performLogin(Scanner scanner, DataOutputStream out, DataInputStream in) {
        try {
            System.out.println("---- LOG IN ----");
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine();

            out.writeUTF("LOGIN");
            out.writeUTF(username);
            out.writeUTF(password);
            out.flush();

            String response = in.readUTF();
            if ("LOGIN_RESULT".equals(response)) {
                boolean ok = in.readBoolean();
                String msg = in.readUTF();
                System.out.println("Server: " + msg);
                return ok ? username : null;
            } else {
                System.err.println("Unexpected server response: " + response);
                return null;
            }
        } catch (IOException e) {
            System.err.println("I/O error during login: " + e.getMessage());
            return null;
        }
    }

    //SYMPTOMS
    private static void sendSymptomsInteractive(Scanner scanner, DataOutputStream out, DataInputStream in) {
        try {
            System.out.println("\nSelect symptoms from the list (IDs). Example input: 1,3,5");
            System.out.println("1 - Pain\n2 - Difficulty holding objects\n3 - Trouble breathing\n4 - Trouble swallowing\n5 - Trouble sleeping\n6 - Fatigue");
            System.out.print("Enter symptom IDs separated by commas (or leave blank for none): ");
            String line = scanner.nextLine().trim();
            String[] tokens = line.isEmpty() ? new String[0] : line.split(",");

            out.writeUTF("SYMPTOMS");
            out.writeInt(tokens.length);
            for (String t : tokens) {
                try {
                    int id = Integer.parseInt(t.trim());
                    out.writeInt(id);
                } catch (NumberFormatException nfe) {
                    out.writeInt(-1);
                }
            }
            out.writeUTF(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
            out.flush();

            String response = in.readUTF();
            if ("ACK".equals(response)) {
                String msg = in.readUTF();
                System.out.println("Server: " + msg);
            } else {
                System.err.println("Server response: " + response);
            }
        } catch (IOException e) {
            System.err.println("I/O error sending symptoms: " + e.getMessage());
        }
    }

    // HELPERS

    private static void setFieldIfExists(Object target, String name, Object value) {
        if (target == null || name == null) return;
        Class<?> cls = target.getClass();
        String setterName = "set" + name;

        try {
            // cerca setter (case-insensitive)
            for (Method m : cls.getMethods()) {
                if (m.getName().equalsIgnoreCase(setterName) && m.getParameterCount() == 1) {
                    Object arg = convertToParameterType(value, m.getParameterTypes()[0]);
                    m.invoke(target, arg);
                    return;
                }
            }

            try {
                java.lang.reflect.Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                Object arg = convertToParameterType(value, f.getType());
                f.set(target, arg);
            } catch (NoSuchFieldException ignored) {
            }

        } catch (Throwable ignored) {
            }
    }

    @SuppressWarnings("unchecked")
    private static Object convertToParameterType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) return value;

        String s = value.toString();
        try {
            if (targetType == String.class) return s;

            if (targetType == Integer.class || targetType == int.class) {
                if (value instanceof Number) return ((Number) value).intValue();
                return Integer.parseInt(s);
            }
            if (targetType == Long.class || targetType == long.class) {
                if (value instanceof Number) return ((Number) value).longValue();
                return Long.parseLong(s);
            }
            if (targetType == java.time.LocalDateTime.class) {
                if (value instanceof java.time.LocalDateTime) return value;
                if (value instanceof java.time.LocalDate) return ((java.time.LocalDate) value).atStartOfDay();
                return java.time.LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            if (targetType == java.time.LocalDate.class) {
                if (value instanceof java.time.LocalDate) return value;
                if (value instanceof java.time.LocalDateTime) return ((java.time.LocalDateTime) value).toLocalDate();
                return java.time.LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (java.util.Date.class.isAssignableFrom(targetType)) {
                if (value instanceof java.util.Date) return value;
                if (value instanceof java.time.LocalDateTime) {
                    return java.util.Date.from(((java.time.LocalDateTime) value).atZone(java.time.ZoneId.systemDefault()).toInstant());
                }
                if (value instanceof java.time.LocalDate) {
                    return java.util.Date.from(((java.time.LocalDate) value).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                }

                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return java.util.Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant());
            }

            if (java.util.List.class.isAssignableFrom(targetType)) {
                if (value instanceof java.util.List) return value;
                // se è una stringa separata da virgole, creiamo una ArrayList
                java.util.List<String> list = new java.util.ArrayList<>();
                if (s != null && !s.isBlank()) {
                    for (String part : s.split(",")) {
                        String t = part.trim();
                        if (!t.isEmpty()) list.add(t);
                    }
                }
                return list;
            }

            if (targetType.isEnum()) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object e = Enum.valueOf((Class<Enum>) targetType, s);
                return e;
            }

        } catch (Exception ignored) {
            // fallback al valore originale
        }
        return value;
    }

    // java
    public static List<DiagnosisFile> parseDiagnosisFileList(String payload) {
        List<DiagnosisFile> files = new ArrayList<>();
        if (payload == null) return files;
        payload = payload.trim();
        if (payload.isEmpty() || payload.equals("[]")) return files;

        int pos = 0;
        while (true) {
            int start = payload.indexOf("MedicalRecord{", pos);
            if (start < 0) break;
            start = payload.indexOf('{', start);
            if (start < 0) break;
            int brace = start + 1;
            int depth = 1;
            while (brace < payload.length() && depth > 0) {
                char c = payload.charAt(brace);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                brace++;    // brace increases until I reach the } position that closes the MedicalRecord{
            }
            if (depth != 0) break;
            String body = payload.substring(start + 1, brace - 1).trim();
            pos = brace;

            // parse key=value pairs in body
            Map<String, String> map = new LinkedHashMap<>();
            int i = 0;
            while (i < body.length()) {
                // read key
                int eq = body.indexOf('=', i);
                if (eq < 0) break;
                String key = body.substring(i, eq).trim();
                i = eq + 1;

                // read value: could start with ' (quoted), [ (list), or plain until comma
                String value;
                if (i < body.length() && body.charAt(i) == '\'') {
                    // quoted string
                    i++; // skip '
                    StringBuilder sb = new StringBuilder();
                    while (i < body.length()) {
                        char c = body.charAt(i++);
                        if (c == '\'') break;
                        if (c == '\\' && i < body.length()) { // support escaped chars
                            sb.append(body.charAt(i++));
                        } else {
                            sb.append(c);
                        }
                    }
                    value = sb.toString();
                    // skip optional comma and space
                    while (i < body.length() && (body.charAt(i) == ',' || Character.isWhitespace(body.charAt(i)))) i++;
                } else if (i < body.length() && body.charAt(i) == '[') {
                    // bracketed list - read until matching ]
                    int j = i;
                    int d = 0;
                    StringBuilder sb = new StringBuilder();
                    while (j < body.length()) {
                        char c = body.charAt(j);
                        if (c == '[') d++;
                        else if (c == ']') {
                            d--;
                            if (d == 0) { sb.append(c); j++; break; }
                        }
                        sb.append(c);
                        j++;
                    }
                    value = sb.toString();
                    i = j;
                    // skip optional comma and spaces
                    while (i < body.length() && (body.charAt(i) == ',' || Character.isWhitespace(body.charAt(i)))) i++;
                } else {
                    // plain token until comma
                    int comma = i;
                    while (comma < body.length() && body.charAt(comma) != ',') comma++;
                    value = body.substring(i, comma).trim();
                    i = comma + 1;
                    while (i < body.length() && Character.isWhitespace(body.charAt(i))) i++;
                }
                map.put(key, value);
            }

            // costruisci DiagnosisFile usando setFieldIfExists
            DiagnosisFile df = new DiagnosisFile();
            try {
                // id (può essere "id='123'" oppure "id=123")
                String idStr = map.get("id");
                if (idStr != null) {
                    idStr = idStr.replace("'", "").trim();
                    try { setFieldIfExists(df, "id", Integer.parseInt(idStr)); } catch (Exception e) { setFieldIfExists(df, "id", idStr); }
                }

                // symptoms: può essere "['a','b']" o "[a, b]" o "a,b"
                String symptomsRaw = map.get("symptoms");
                if (symptomsRaw != null) {
                    String s = symptomsRaw.trim();
                    List<String> symptoms = new ArrayList<>();
                    if (s.startsWith("[")) {
                        // rimuovi [ ]
                        String inner = s.substring(1, Math.max(1, s.length()-1));
                        // split rispettando apici
                        StringBuilder cur = new StringBuilder();
                        boolean inQuote = false;
                        for (int k = 0; k < inner.length(); k++) {
                            char c = inner.charAt(k);
                            if (c == '\'' || c == '\"') {
                                inQuote = !inQuote;
                                continue;
                            }
                            if (c == ',' && !inQuote) {
                                String t = cur.toString().trim();
                                if (!t.isEmpty()) symptoms.add(t);
                                cur.setLength(0);
                            } else {
                                cur.append(c);
                            }
                        }
                        String last = cur.toString().trim();
                        if (!last.isEmpty()) symptoms.add(last);
                    } else {
                        // comma separated plain
                        for (String part : s.split(",")) {
                            String t = part.replace("'", "").replace("\"","").trim();
                            if (!t.isEmpty()) symptoms.add(t);
                        }
                    }
                    setFieldIfExists(df, "symptoms", symptoms);
                }

                // diagnosis, medication
                if (map.containsKey("diagnosis")) setFieldIfExists(df, "diagnosis", map.get("diagnosis").replace("'", "").trim());
                if (map.containsKey("medication")) setFieldIfExists(df, "medication", map.get("medication").replace("'", "").trim());

                // date
                if (map.containsKey("date")) {
                    String dateRaw = map.get("date").replace("'", "").trim();
                    if (!dateRaw.isEmpty()) {
                        // tenta ISO_LOCAL_DATE_TIME e fallback a stringa
                        try {
                            LocalDateTime ldt = LocalDateTime.parse(dateRaw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            setFieldIfExists(df, "date", ldt);
                        } catch (Exception ex) {
                            setFieldIfExists(df, "date", dateRaw);
                        }
                    }
                }

                // patient id
                if (map.containsKey("patient id")) {
                    String pid = map.get("patient id").replace("'", "").trim();
                    try { setFieldIfExists(df, "patientId", Integer.parseInt(pid)); } catch (Exception e) { setFieldIfExists(df, "patientId", pid); }
                } else if (map.containsKey("patientId")) {
                    String pid = map.get("patientId").replace("'", "").trim();
                    try { setFieldIfExists(df, "patientId", Integer.parseInt(pid)); } catch (Exception e) { setFieldIfExists(df, "patientId", pid); }
                }

                // status
                if (map.containsKey("status")) {
                    String statusStr = map.get("status").replace("'", "").trim();
                    try { setFieldIfExists(df, "status", Boolean.parseBoolean(statusStr)); } catch (Exception e) { setFieldIfExists(df, "status", statusStr); }
                }

            } catch (Throwable ignored) {}

            files.add(df);
        }

        // ordina come prima
        files.sort(java.util.Comparator.comparing(DiagnosisFile::getDate,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed());

        return files;
    }


    public static List<Patient> parsePatientList(String payload) {
        List<Patient> patients = new ArrayList<>();
        if (payload == null) return patients;
        payload = payload.trim();
        if (payload.isEmpty()) return patients;

        int pos = 0;
        while (true) {
            int start = payload.indexOf("Patient{", pos);
            if (start < 0) break;
            start = payload.indexOf('{', start);
            if (start < 0) break;
            int brace = start + 1;
            int depth = 1;
            while (brace < payload.length() && depth > 0) {
                char c = payload.charAt(brace);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                brace++;
            }
            if (depth != 0) break;
            String body = payload.substring(start + 1, brace - 1).trim();
            pos = brace;

            // parse key=value pairs
            Map<String, String> map = new LinkedHashMap<>();
            int i = 0;
            while (i < body.length()) {
                int eq = body.indexOf('=', i);
                if (eq < 0) break;
                String key = body.substring(i, eq).trim();
                i = eq + 1;

                String value;
                if (i < body.length() && body.charAt(i) == '\'') {
                    // quoted string with single quote
                    i++; // skip '
                    StringBuilder sb = new StringBuilder();
                    while (i < body.length()) {
                        char ch = body.charAt(i++);
                        if (ch == '\'') break;
                        if (ch == '\\' && i < body.length()) {
                            sb.append(body.charAt(i++));
                        } else {
                            sb.append(ch);
                        }
                    }
                    value = sb.toString();
                    while (i < body.length() && (body.charAt(i) == ',' || Character.isWhitespace(body.charAt(i)))) i++;
                } else if (i < body.length() && body.charAt(i) == '[') {
                    // bracketed list
                    int j = i;
                    int d = 0;
                    StringBuilder sb = new StringBuilder();
                    while (j < body.length()) {
                        char ch = body.charAt(j);
                        sb.append(ch);
                        if (ch == '[') d++;
                        else if (ch == ']') {
                            d--;
                            if (d == 0) { j++; break; }
                        }
                        j++;
                    }
                    value = sb.toString();
                    i = j;
                    while (i < body.length() && (body.charAt(i) == ',' || Character.isWhitespace(body.charAt(i)))) i++;
                } else {
                    // plain until comma
                    int comma = i;
                    while (comma < body.length() && body.charAt(comma) != ',') comma++;
                    value = body.substring(i, comma).trim();
                    i = comma + 1;
                    while (i < body.length() && Character.isWhitespace(body.charAt(i))) i++;
                }
                map.put(key, value);
            }

            // build Patient
            Patient p = new Patient();
            try {
                if (map.containsKey("idPatient")) {
                    String v = map.get("idPatient").replace("'", "").trim();
                    try { setFieldIfExists(p, "idPatient", Integer.parseInt(v)); } catch (Exception e) { setFieldIfExists(p, "idPatient", v); }
                }
                if (map.containsKey("namePatient")) setFieldIfExists(p, "namePatient", map.get("namePatient").replace("'", "").trim());
                if (map.containsKey("dniPatient")) setFieldIfExists(p, "dniPatient", map.get("dniPatient").replace("'", "").trim());
                if (map.containsKey("dobPatient")) {
                    String dob = map.get("dobPatient").replace("'", "").trim();
                    if (!dob.isEmpty()) {
                        // prova ISO_LOCAL_DATE e fallback a stringa
                        try {
                            java.time.LocalDate ld = java.time.LocalDate.parse(dob, DateTimeFormatter.ISO_LOCAL_DATE);
                            setFieldIfExists(p, "dobPatient", ld);
                        } catch (Exception ex) {
                            setFieldIfExists(p, "dobPatient", dob);
                        }
                    }
                }
                if (map.containsKey("emailPatient")) setFieldIfExists(p, "emailPatient", map.get("emailPatient").replace("'", "").trim());
                if (map.containsKey("sexPatient")) setFieldIfExists(p, "sexPatient", map.get("sexPatient").replace("'", "").trim());
                if (map.containsKey("phoneNumberPatient")) {
                    String v = map.get("phoneNumberPatient").replace("'", "").trim();
                    try { setFieldIfExists(p, "phoneNumberPatient", Long.parseLong(v)); } catch (Exception e) { setFieldIfExists(p, "phoneNumberPatient", v); }
                }
                if (map.containsKey("healthInsuranceNumberPatient")) {
                    String v = map.get("healthInsuranceNumberPatient").replace("'", "").trim();
                    try { setFieldIfExists(p, "healthInsuranceNumberPatient", Integer.parseInt(v)); } catch (Exception e) { setFieldIfExists(p, "healthInsuranceNumberPatient", v); }
                }
                if (map.containsKey("emergencyContactPatient")) setFieldIfExists(p, "emergencyContactPatient", map.get("emergencyContactPatient").replace("'", "").trim());
                if (map.containsKey("doctorId")) {
                    String v = map.get("doctorId").replace("'", "").trim();
                    try { setFieldIfExists(p, "doctorId", Integer.parseInt(v)); } catch (Exception e) { setFieldIfExists(p, "doctorId", v); }
                }
                if (map.containsKey("MACadress")) setFieldIfExists(p, "MACadress", map.get("MACadress").replace("'", "").trim());

                // diagnosisList: usa il parser dedicato per ottenere List<DiagnosisFile>
                if (map.containsKey("diagnosisFile") || map.containsKey("diagnosisList")) {
                    String key = map.containsKey("diagnosisFile") ? "diagnosisFile" : "diagnosisList";
                    String raw = map.get(key).trim();
                    List<DiagnosisFile> diagFiles = parseDiagnosisFileList(raw);
                    setFieldIfExists(p, "diagnosisList", diagFiles);
                }

                if (map.containsKey("userId")) {
                    String v = map.get("userId").replace("'", "").trim();
                    try { setFieldIfExists(p, "userId", Integer.parseInt(v)); } catch (Exception e) { setFieldIfExists(p, "userId", v); }
                }
            } catch (Throwable ignored) {}

            patients.add(p);
        }

        return patients;
    }




    //----------------------------------------- METHODS-------------------------------------------------------

    public static List<DiagnosisFile> listRecentlyFinishDiagFilesToDo(DataOutputStream out, DataInputStream in, int doctorId) {
        List<DiagnosisFile> files = new ArrayList<>();
        try {
            out.writeUTF("RECENTLY_FINISH");
            String resp = in.readUTF();
            if (!"RECENTLY_FINISH_LIST".equals(resp)) {
                System.err.println("Unexpected response: " + resp);
                return files;
            }
                String listDFstring = in.readUTF();
                files = parseDiagnosisFileList(listDFstring);

        } catch (IOException e) {
            System.err.println("I/O error while listing recently finished diagnosis files: " + e.getMessage());
        }
        return files;
    }


    private static String [] sendDiagnosisAsString(Scanner scanner, DataOutputStream out, DataInputStream in, DiagnosisFile file) {
        if (file == null) return new String[] {"ERROR", "Diagnosis file is null"};
        try {
            out.writeUTF("COMPLETE_DIAGNOSIS_FILE");
            String resp = in.readUTF();
            if(!"COMPLETE_DIAGNOSISFILE_READY".equals(resp)){
                return new String[] {"ERROR", "Server not ready to receive COMPLETE_DIAGNOSIS_FILE"};
            }

            Integer idFile = null;
            try { idFile = file.getId(); } catch (Throwable ignored) {}
            if (idFile == null) {
                return new String[] {"ERROR", "Diagnosis file id is null"};
            }
            String idDF = String.valueOf(idFile);

            out.writeUTF(idDF);
            System.out.println("Insert diagnosis:");
            String inputDiag = scanner.nextLine();
            if (inputDiag != null && !inputDiag.isBlank()) {
                out.writeUTF(inputDiag);
            } else {
                System.out.println("Invalid diagnosis");
            }
            String finalResp = in.readUTF();
            if("!COMPLETE_DIAGNOSISFILE_SAVED".equals(finalResp)){
                return new String[] {"ERROR", "Diagnosis was not saved successfully"};
            } return new String[] {"OK", "Diagnosis saved successfully"};


        } catch (IOException e) {
            System.err.println("I/O error while sending diagnosis: " + e.getMessage());
            return new String[] {"ERROR", "I/O error while sending diagnosis: " + e.getMessage()};
        }
    }

    private static String[] getAllHIN(DataOutputStream out, DataInputStream in) throws IOException {
        out.writeUTF("SEARCH_PATIENT");
        out.flush();
        String listHIN = null;
        listHIN = in.readUTF();
        if (listHIN == null || listHIN.isBlank()) {
            return new String[0];
        }
        listHIN = listHIN.trim();
        if (listHIN.isEmpty()) {
            return new String[0];
        }
        String[] hinArray = listHIN.split("\\s*,\\s*");
        return Arrays.stream(hinArray).map(String::trim).toArray(String[]::new);
    }

    private static Patient getPatientInfoByHIN(Scanner scanner, DataOutputStream out, DataInputStream in){
        Patient patient = null;
        try{
            out.writeUTF("VIEW_PATIENT");
            out.flush();
            System.out.println("Insert health insurance number of the patient:");
            String hin = scanner.nextLine();
            out.writeInt(Integer.parseInt(hin));
            String resp = in.readUTF();
            if(!"PATIENT_OVERVIEW_SENT".equals(resp)){
                System.err.println("Unexpected response: " + resp);
            }
            String patientString = in.readUTF();
            if(patientString == null || patientString.isBlank()){
                System.err.println("Received empty patient data");
                return null;
            }
            patient = parsePatientList(patientString).stream().findFirst().orElse(null);
            return patient;
        } catch(IOException e){
            System.err.println("I/O error while getting patient info: " + e.getMessage());
            return null;
        }
    }

    private static DiagnosisFile selectAndViewDIagnosisFile(Patient patient ,int idDiagnosisFile){
        if (patient == null) return null;
        List<DiagnosisFile> diagnosisList = patient.getDiagnosisList();
        if (diagnosisList == null || diagnosisList.isEmpty()) return null;

        Integer target = Integer.valueOf(idDiagnosisFile);
        for (DiagnosisFile df : diagnosisList) {
            if (df == null) continue;
            Integer idDF = null;
            try { idDF = df.getId(); } catch (Throwable ignored) {}
            if (Objects.equals(idDF, target)) return df;
        }
        return null;
    }

    private static void downloadDiagnosisFile(Scanner scanner, DataOutputStream out, DataInputStream in){
        try {
            out.writeUTF("DOWNLOAD_DIAGNOSISFILE");
            out.flush();
            String resp = in.readUTF();
            if (!"DOWNLOAD_DIAGNOSISFILE_STARTED".equals(resp)) {
                System.err.println("Unexpected response: " + resp);
                return;
            }
            String diagnosisId = scanner.nextLine();
            if (diagnosisId == null) {
                System.err.println("Invalid diagnosis id");
                return;
            }
            out.writeUTF(diagnosisId);
            String diagnosisFileString = in.readUTF();

            String userHome = System.getProperty("user.home");
            java.nio.file.Path downloads = java.nio.file.Paths.get(userHome, "Downloads");
            try {
                java.nio.file.Files.createDirectories(downloads);
            } catch (IOException e) {
                System.err.println("Cannot create Downloads folder: " + e.getMessage());
                return;
            }

            System.out.println("Downloading Diagnosis File ID: " + diagnosisId);
            String fileName = "diagnosis_" + diagnosisId + ".txt";
            java.nio.file.Path outfile = downloads.resolve(fileName);

            try (java.io.BufferedWriter bw = java.nio.file.Files.newBufferedWriter(outfile,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                bw.write(diagnosisFileString);
                bw.flush();
                System.out.println("Saved diagnosis text to " + out.toString());
            } catch (IOException e) {
                System.err.println("Error saving diagnosis file: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Cannot receive diagnosis file from server: " + e.getMessage());
        }
    }


    private void viewRecording(String diagnosisFileId, DataOutputStream out, DataInputStream in) throws IOException {
        out.writeUTF("VIEW_RECORDING");

        String message = diagnosisFileId + ",1";
        out.writeUTF(message);
        out.flush();

        String fragment = in.readUTF();
        String statesString = in.readUTF();

        List<Boolean> stateList = new ArrayList<>();
        if (!statesString.isEmpty()) {
            String[] parts = statesString.split(",");
            for (String p : parts) {
                stateList.add(Boolean.parseBoolean(p.trim()));
            }
        }
    }

    private void changeFragment(String diagnosisFileId, int sequence, DataOutputStream out, DataInputStream in) throws IOException {
        // 1. Enviar comando al servidor
        out.writeUTF("CHANGE_FRAGMENT");

        String message = diagnosisFileId + "," + sequence;
        out.writeUTF(message);
        out.flush();

        String fragment = in.readUTF();
        String confirmation = in.readUTF();
    }

    private void downloadRecording(String diagnosisFileId, DataOutputStream out, DataInputStream in) throws IOException {
        out.writeUTF("DOWNLOAD_RECORDING");
        out.writeUTF(diagnosisFileId);
        out.flush();
        String status = in.readUTF();

        // Si no es el mensaje esperado, salimos sin hacer nada más
        if (!"SENDING_RECORDING".equals(status)) {
            return;
        }

        String ecgString = in.readUTF();
        String edaString = in.readUTF();

        String[] ecgValues = ecgString.split(",");
        String[] edaValues = edaString.split(",");
        int length = Math.min(ecgValues.length, edaValues.length);

        // 6. Crear el CSV: una columna ECG y otra EDA
        String fileName = "recording_" + diagnosisFileId + ".csv";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Cabecera
            writer.write("ECG,EDA");
            writer.newLine();
            // Datos fila a fila
            for (int i = 0; i < length; i++) {
                String ecgSample = ecgValues[i].trim();
                String edaSample = edaValues[i].trim();

                writer.write(ecgSample + "," + edaSample);
                writer.newLine();
            }
        }
    }


    private static int[] getFragmentOfRecording(DataOutputStream out, DataInputStream in, DiagnosisFile df, int fragmentIndex, int length) {
        if (df == null) {
            System.err.println("DiagnosisFile is null");
            return new int[0];
        }
        Integer diagnosisId = null;
        try { diagnosisId = df.getId(); } catch (Throwable ignored) {}
        if (diagnosisId == null || diagnosisId < 0) {
            System.err.println("Invalid diagnosis id");
            return new int[0];
        }

        if (fragmentIndex < 0 || length <= 0) {
            System.err.println("Invalid fragmentIndex or length");
            return new int[0];
        }

        try {

            out.writeUTF("GET_FRAGMENT_OF_RECORDING");
            out.writeInt(diagnosisId);
            out.writeInt(fragmentIndex);
            out.writeInt(length);
            out.flush();


            String responseType = in.readUTF();
            if ("FRAGMENTS".equals(responseType)) {
                int size = in.readInt();
                if (size < 0) {
                    System.err.println("Server returned negative fragment size: " + size);
                    return new int[0];
                }
                final int MAX_ACCEPTABLE = Math.max(length, 100_000);
                if (size > MAX_ACCEPTABLE) {
                    System.err.println("Fragment size too large: " + size);
                    // consume/skip or return empty
                    return new int[0];
                }

                int[] fragments = new int[size];
                for (int i = 0; i < size; i++) {
                    fragments[i] = in.readInt();
                }
                return fragments;
            } else if ("ERROR".equals(responseType)) {
                String msg = in.readUTF();
                System.err.println("Server error when requesting fragment: " + msg);
                return new int[0];
            } else {
                System.err.println("Unexpected server response type for fragments: " + responseType);
                return new int[0];
            }
        } catch (IOException e) {
            System.err.println("I/O error while requesting fragments of recording: " + e.getMessage());
            return new int[0];
        }
    }


    private static List<Boolean> getStateOfFragmentsOfRecordingByID(DataOutputStream out, DataInputStream in, int diagnosisFileId, int[] fragmentIds) {
        List<Boolean> states = new ArrayList<>();
        if (fragmentIds == null) return states;

        try {

            out.writeUTF("GET_FRAGMENT_STATES");
            out.writeInt(diagnosisFileId);
            out.writeInt(fragmentIds.length);
            for (int id : fragmentIds) {
                out.writeInt(id);
            }
            out.flush();


            String responseType = in.readUTF();
            if (!"FRAGMENT_STATES".equals(responseType)) {
                System.err.println("Unexpected server response type for fragment states: " + responseType);
                return states;
            }

            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                states.add(in.readBoolean());
            }
        } catch (IOException e) {
            System.err.println("I/O error while requesting fragment states: " + e.getMessage());
        }
        return states;
    }

    private static List<DiagnosisFile> getAllDiagnosisFilesFromPatientId(DataOutputStream out, DataInputStream in, int patientId) {
        List<DiagnosisFile> files = new ArrayList<>();
        try {
            out.writeUTF("GET_DIAGNOSIS_FILES_BY_PATIENT_ID");
            out.writeInt(patientId);
            out.flush();

            String resp = in.readUTF();
            if (!"DIAGNOSIS_FILES".equals(resp)) {
                System.err.println("Unexpected response: " + resp);
                return files;
            }
            int total = in.readInt();
            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            for (int i = 0; i < total; i++) {
                int id = in.readInt();
                String symptomsStr = in.readUTF();
                String diagnosis = in.readUTF();
                String medication = in.readUTF();
                String dateStr = in.readUTF();

                LocalDateTime date = null;
                try {
                    if (dateStr != null && !dateStr.isEmpty()) {
                        date = LocalDateTime.parse(dateStr, fmt);
                    }
                } catch (Exception ex) {
                    date = null;
                }

                DiagnosisFile df = new DiagnosisFile();

                ArrayList<String> symptoms = new ArrayList<>();
                if (symptomsStr != null && !symptomsStr.isBlank()) {
                    for (String s : symptomsStr.split(",")) {
                        String t = s.trim();
                        if (!t.isEmpty()) symptoms.add(t);
                    }
                }

                setFieldIfExists(df, "id", id);
                setFieldIfExists(df, "date", date); // se POJO ha LocalDate oppure LocalDateTime o String, verrà convertito
                setFieldIfExists(df, "patientId", patientId);
                setFieldIfExists(df, "symptoms", symptoms);
                setFieldIfExists(df, "diagnosis", diagnosis);
                setFieldIfExists(df, "medication", medication);

                files.add(df);
            }
            files.sort(java.util.Comparator.comparing(DiagnosisFile::getDate,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                    .reversed());
        } catch (IOException e) {
            System.err.println("I/O error while requesting diagnosis files by patient ID: " + e.getMessage());
        }
        return files;
    }


    private static void releaseResources(Socket socket, DataOutputStream outputStream, Scanner scanner, DataInputStream inputStream) {
        if (scanner != null) scanner.close();

        try { if (outputStream != null) outputStream.close(); }
        catch (IOException e) {
            Logger.getLogger(DoctorServerConnection.class.getName()).log(Level.SEVERE, "Error closing OutputStream", e);
        }
        try { if (inputStream != null) inputStream.close(); }
        catch (IOException e) {
            Logger.getLogger(DoctorServerConnection.class.getName()).log(Level.SEVERE, "Error closing InputStream", e);
        }
        try { if (socket != null) socket.close(); }
        catch (IOException e) {
            Logger.getLogger(DoctorServerConnection.class.getName()).log(Level.SEVERE, "Error closing socket", e);
        }
    }
}
