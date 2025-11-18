
package executable;

import bitalino.BITalino;
import bitalino.BITalinoException;
import bitalino.BitalinoManager;
import bitalino.Frame;
import common.enums.Sex;
import pojos.DiagnosisFile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.List;
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
        String s = normalizeDNI(dni);
        if (s == null) return false;

        // Soportar NIE: X/Y/Z -> 0/1/2 delante de los 7 dígitos restantes
        if (s.matches("[XYZ]\\d{7}[A-Z]")) {
            char first = s.charAt(0);
            String prefix = first == 'X' ? "0" : first == 'Y' ? "1" : "2";
            s = prefix + s.substring(1); // ahora s tiene 8 dígitos + letra
        }

        if (!s.matches("\\d{8}[A-Z]")) {
            System.err.println("Formato DNI inválido tras normalizar: \"" + s + "\"");
            return false;
        }

        final String LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
        int number;
        try {
            number = Integer.parseInt(s.substring(0, 8));
        } catch (NumberFormatException e) {
            System.err.println("Los primeros 8 caracteres no son un número válido: \"" + s.substring(0,8) + "\"");
            return false;
        }

        char expected = LETTERS.charAt(number % 23);
        char provided = s.charAt(8);

        if (provided != expected) {
            System.err.println("DNI inválido. Normalizado: " + s +
                    ". Letra esperada: " + expected +
                    " (número % 23 = " + (number % 23) + "), letra proporcionada: " + provided);
            return false;
        }

        return true;
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
        String MACAddress;
        Socket socket = null;
        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;
        BITalino bitalino = null;
        BitalinoManager bitalinoManager = new BitalinoManager();


        while (true) {
            System.out.println("Enter the IP address (or 'localhost'): ");
            serverAddress = scanner.nextLine();
            if (isValidIPAddress(serverAddress)) break;
            System.out.println("Invalid IP address format. Please try again.");
        }


        while (true) {
            System.out.print("Enter MAC address (XX:XX:XX:XX:XX:XX): ");
            MACAddress = scanner.nextLine();
            if (BitalinoManager.isValidMacAddress(MACAddress)) break;
            System.out.println("Invalid MAC address. Please enter a valid MAC address.");
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

            // BITalino
            bitalino = new BITalino();
            int samplingRate = 1000;

            boolean done = false;
            while (!done) {
                System.out.println("\nReady to record. Press ENTER to start recording or type Q + ENTER to quit.");
                String line = scanner.nextLine();
                if (line.equalsIgnoreCase("Q")) {
                    System.out.println("Quitting application.");
                    done = true;
                    break;
                }
                outputStream.writeUTF("START");
                outputStream.flush();

                try {
                    bitalino.open(MACAddress, samplingRate);
                    int[] channelsToAcquire = new int[]{1, 2};
                    bitalino.start(channelsToAcquire);
                    System.out.println("Recording started. Press ENTER to stop recording.");
                } catch (Throwable ex) {
                    System.err.println("Error starting BITalino: " + ex.getMessage());
                    outputStream.writeUTF("ERROR");
                    outputStream.writeUTF("BITalino open/start failed: " + ex.getMessage());
                    outputStream.flush();
                    try { bitalino.close(); } catch (Throwable ignored) {}
                    continue;
                }

                Thread stopper = new Thread(() -> {
                    try { System.in.read(); } catch (IOException ignored) {}
                });
                stopper.start();

                int blockSize = 10;
                long blockNumber = 0;
                try {
                    while (stopper.isAlive()) {
                        Frame[] frames = bitalino.read(blockSize);
                        for (Frame f : frames) {
                            outputStream.writeUTF("DATA");
                            outputStream.writeInt((int) blockNumber);
                            outputStream.writeInt(f.seq);
                            outputStream.writeInt(f.analog[1]);
                            outputStream.writeInt(f.analog[2]);
                            outputStream.flush();
                            blockNumber++;
                        }
                    }
                } catch (Throwable ex) {
                    System.err.println("Error while recording/streaming frames: " + ex.getMessage());
                    try {
                        outputStream.writeUTF("ERROR");
                        outputStream.writeUTF("Exception during recording: " + ex.getMessage());
                        outputStream.flush();
                    } catch (IOException ignored) {}
                } finally {
                    try { bitalino.stop(); bitalino.close(); } catch (Throwable ignored) {}
                }

                outputStream.writeUTF("END");
                outputStream.flush();

                try {
                    String serverResponse = inputStream.readUTF();
                    System.out.println("Server: " + serverResponse);
                } catch (IOException e) {
                    System.out.println("No ACK received (server may have disconnected).");
                }

                sendSymptomsInteractive(scanner, outputStream, inputStream);

                System.out.println("Do you want to record again? (yes/no)");
                String again = scanner.nextLine().trim().toLowerCase();
                if (!again.equals("yes") && !again.equals("y")) {
                    done = true;
                }
            }

        } catch (Throwable e) {
            Logger.getLogger(DoctorServerConnection.class.getName()).log(Level.SEVERE, "Error in the client", e);
        } finally {
            releaseResources(bitalino, socket, outputStream, scanner, inputStream);
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
                System.out.print("Birthday (yyyy-MM-dd): ");
                birthday = scanner.nextLine().trim();
                if (birthday.matches("\\d{4}-\\d{2}-\\d{2}")) break;
                System.out.println("Invalid format. Use yyyy-MM-dd.");
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
            out.writeUTF(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
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


    // methods
    public static List<DiagnosisFile> listRecentlyFinishDiagFilesTODO(DataOutputStream out, DataInputStream in, int doctorId) {
        List<DiagnosisFile> files = new ArrayList<>();
        try {
            out.writeUTF("LIST_RECENT_DIAGNOSIS_FILES");
            out.writeInt(doctorId);
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
                int patientId = in.readInt();

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
            System.err.println("I/O error while requesting recent diagnosis files: " + e.getMessage());
        }
        return files;
    }


    private static String[] sendUpdatedDiagnosisFile(DataOutputStream out, DataInputStream in, int doctorId, DiagnosisFile file) {
        if (file == null) return new String[0];
        try {
            out.writeUTF("SEND_UPDATED_DIAGNOSIS_FILE");
            out.writeInt(doctorId);
            Integer id = null;
            try { id = file.getId(); } catch (Throwable ignored) {}
            out.writeInt(id == null ? -1 : id);

            String symptomsStr = "";
            try {
                List<String> symptoms = file.getSymptoms();
                if (symptoms != null && !symptoms.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String s : symptoms) {
                        if (s == null) continue;
                        if (sb.length() > 0) sb.append(",");
                        sb.append(s.trim());
                    }
                    symptomsStr = sb.toString();
                }
            } catch (Throwable ignored) {}
            out.writeUTF(symptomsStr);

            String diagnosis = "";
            try { diagnosis = file.getDiagnosis(); } catch (Throwable ignored) {}
            out.writeUTF(diagnosis);

            String medication = "";
            try { medication = file.getMedication() == null ? "" : file.getMedication(); } catch (Throwable ignored) {}
            out.writeUTF(medication);

            String dateStr = "";
            try {
                Object dateObj = null;
                try { dateObj = file.getDate(); } catch (Throwable ignored) {}
                if (dateObj != null) {
                    if (dateObj instanceof LocalDateTime) {
                        dateStr = ((LocalDateTime) dateObj).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } else if (dateObj instanceof LocalDate) {
                        dateStr = ((LocalDate) dateObj).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } else if (dateObj instanceof Date) {
                        java.time.Instant inst = ((Date) dateObj).toInstant();
                        dateStr = LocalDateTime.ofInstant(inst, java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } else {
                        dateStr = dateObj.toString();
                    }
                }
            } catch (Throwable ignored) {}
            out.writeUTF(dateStr);

            Integer patientId = null;
            try { patientId = file.getPatientId(); } catch (Throwable ignored) {}
            out.writeInt(patientId == null ? -1 : patientId);

            out.flush();

            String resp = in.readUTF();
            if ("ACK".equals(resp)) {
                String msg = in.readUTF();
                return new String[] {"ACK", msg};
            } else if ("ERROR".equals(resp)) {
                String msg = in.readUTF();
                return new String[] {"ERROR", msg};
            } else {
                return new String[] {"UNKNOWN", resp};
            }

        } catch (IOException e) {
            return new String[] {"ERROR", e.getMessage()};
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



    private static void releaseResources(BITalino bitalino, Socket socket, DataOutputStream outputStream, Scanner scanner, DataInputStream inputStream) {
        if (scanner != null) scanner.close();
        try { if (bitalino != null) bitalino.close(); }
        catch (BITalinoException e) {
            Logger.getLogger(DoctorServerConnection.class.getName()).log(Level.SEVERE, "Error closing Bitalino", e);
        }
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
