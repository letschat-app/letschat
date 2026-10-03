package com.letschat.mvp_1.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import com.letschat.mvp_1.DTOs.AssignmentStudentStatusDTO;
import com.letschat.mvp_1.DTOs.ClassroomStudentStatsDTO;
import com.letschat.mvp_1.Models.AssignmentInfo;
import com.letschat.mvp_1.Models.ClassRoomInfo;
import com.letschat.mvp_1.Models.SubmissionInfo;
import com.letschat.mvp_1.Models.UserChatInfo;
import com.letschat.mvp_1.Repositories.AssignmentRepo;
import com.letschat.mvp_1.Repositories.ClassRoomRepo;
import com.letschat.mvp_1.Repositories.SubmissionRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;

import reactor.core.publisher.Mono;

@Service
public class ClassRoomReportService {

    private final AssignmentRepo assignmentRepo;
    private final SubmissionRepo submissionRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final ClassRoomRepo classRoomRepo;

    @Autowired
    private BrevoMailService brevoMailService;

    public ClassRoomReportService(AssignmentRepo assignmentRepo,
                                 SubmissionRepo submissionRepo,
                                 UserChatInfoRepo userChatInfoRepo,
                                 ClassRoomRepo classRoomRepo) {
        this.assignmentRepo = assignmentRepo;
        this.submissionRepo = submissionRepo;
        this.userChatInfoRepo = userChatInfoRepo;
        this.classRoomRepo = classRoomRepo;
    }

    public Mono<List<AssignmentStudentStatusDTO>> getAssignmentStudentStatuses(Long assignmentId) {
        return assignmentRepo.findById(assignmentId)
                .flatMap(assignment -> {
                    String chatId = String.valueOf(assignment.getChatId());
                    Mono<List<UserChatInfo>> studentsMono = userChatInfoRepo.findClassroomStudents(chatId).collectList();
                    Mono<List<SubmissionInfo>> submissionsMono = submissionRepo.findByAssignmentId(assignmentId).collectList();

                    return Mono.zip(studentsMono, submissionsMono)
                            .map(tuple -> {
                                List<UserChatInfo> students = tuple.getT1();
                                List<SubmissionInfo> submissions = tuple.getT2();

                                Map<String, SubmissionInfo> subMap = new HashMap<>();
                                for (SubmissionInfo s : submissions) {
                                    if (s.getUserId() != null) {
                                        subMap.put(s.getUserId(), s);
                                    }
                                }

                                List<AssignmentStudentStatusDTO> result = new ArrayList<>();
                                for (UserChatInfo student : students) {
                                    AssignmentStudentStatusDTO dto = new AssignmentStudentStatusDTO();
                                    dto.setUserId(student.getUserId());
                                    dto.setUserName(student.getUserName() != null ? student.getUserName() : student.getUserId());
                                    dto.setProfilePic(student.getUserProfile());

                                    SubmissionInfo sub = subMap.get(student.getUserId());
                                    if (sub != null) {
                                        dto.setStatus("SUBMITTED");
                                        dto.setSubmittedAt(sub.getTimestamp());
                                        dto.setSubmissionId(sub.getSubmissionId());
                                        dto.setContent(sub.getContent());
                                        dto.setComment(sub.getComment());
                                        dto.setMarks(sub.getMarks());
                                    } else {
                                        dto.setStatus("MISSING");
                                        dto.setSubmittedAt(null);
                                        dto.setSubmissionId(null);
                                        dto.setContent(null);
                                        dto.setComment(null);
                                        dto.setMarks(null);
                                    }
                                    result.add(dto);
                                }
                                return result;
                            });
                });
    }

    public Mono<List<ClassroomStudentStatsDTO>> getClassroomStudentStats(String roomId) {
        return classRoomRepo.findByRoomId(roomId)
                .flatMap(classroom -> {
                    String chatId = String.valueOf(classroom.getChatId());
                    Mono<List<AssignmentInfo>> assignmentsMono = assignmentRepo.findByChatId(chatId).collectList();
                    Mono<List<UserChatInfo>> studentsMono = userChatInfoRepo.findClassroomStudents(chatId).collectList();

                    return Mono.zip(assignmentsMono, studentsMono)
                            .flatMap(tuple -> {
                                List<AssignmentInfo> assignments = tuple.getT1();
                                List<UserChatInfo> students = tuple.getT2();

                                if (assignments.isEmpty() || students.isEmpty()) {
                                    List<ClassroomStudentStatsDTO> emptyList = students.stream().map(s -> 
                                        new ClassroomStudentStatsDTO(s.getUserId(), s.getUserName(), s.getUserProfile(), 0, 0, assignments.size(), 0.0)
                                    ).toList();
                                    return Mono.just(emptyList);
                                }

                                List<Long> assignmentIds = assignments.stream().map(AssignmentInfo::getAssignmentId).toList();
                                return submissionRepo.findAll()
                                        .filter(sub -> assignmentIds.contains(sub.getAssignmentId()))
                                        .collectList()
                                        .map(allSubmissions -> {
                                            Map<String, Integer> completedMap = new HashMap<>();
                                            for (SubmissionInfo sub : allSubmissions) {
                                                if (sub.getUserId() != null) {
                                                    completedMap.put(sub.getUserId(), completedMap.getOrDefault(sub.getUserId(), 0) + 1);
                                                }
                                            }

                                            List<ClassroomStudentStatsDTO> result = new ArrayList<>();
                                            int total = assignments.size();
                                            for (UserChatInfo s : students) {
                                                int completed = completedMap.getOrDefault(s.getUserId(), 0);
                                                int pending = Math.max(0, total - completed);
                                                double pct = total > 0 ? ((double) completed / total) * 100.0 : 0.0;

                                                result.add(new ClassroomStudentStatsDTO(
                                                        s.getUserId(),
                                                        s.getUserName() != null ? s.getUserName() : s.getUserId(),
                                                        s.getUserProfile(),
                                                        completed,
                                                        pending,
                                                        total,
                                                        Math.round(pct * 10.0) / 10.0
                                                ));
                                            }
                                            return result;
                                        });
                            });
                });
    }

    public Mono<byte[]> generateReport(String roomId, String format) {
        String fmt = format != null ? format.toLowerCase() : "pdf";
        return classRoomRepo.findByRoomId(roomId)
                .flatMap(classroom -> {
                    String chatId = String.valueOf(classroom.getChatId());
                    Mono<List<AssignmentInfo>> assignmentsMono = assignmentRepo.findByChatId(chatId).collectList();
                    Mono<List<UserChatInfo>> studentsMono = userChatInfoRepo.findClassroomStudents(chatId).collectList();

                    return Mono.zip(assignmentsMono, studentsMono)
                            .flatMap(tuple -> {
                                List<AssignmentInfo> assignments = tuple.getT1();
                                List<UserChatInfo> students = tuple.getT2();

                                List<Long> assignmentIds = assignments.stream().map(AssignmentInfo::getAssignmentId).toList();
                                return submissionRepo.findAll()
                                        .filter(sub -> assignmentIds.contains(sub.getAssignmentId()))
                                        .collectList()
                                        .map(submissions -> {
                                            Map<String, Map<Long, SubmissionInfo>> matrix = new HashMap<>();
                                            for (SubmissionInfo sub : submissions) {
                                                matrix.computeIfAbsent(sub.getUserId(), k -> new HashMap<>()).put(sub.getAssignmentId(), sub);
                                            }

                                            if ("excel".equals(fmt) || "xlsx".equals(fmt)) {
                                                return buildExcelReport(classroom, assignments, students, matrix);
                                            } else if ("csv".equals(fmt)) {
                                                return buildCsvReport(classroom, assignments, students, matrix);
                                            } else {
                                                return buildPdfReport(classroom, assignments, students, matrix);
                                            }
                                        });
                            });
                });
    }

    public Mono<Boolean> sendReportByEmail(String roomId, String format, String recipientEmail) {
        return classRoomRepo.findByRoomId(roomId)
                .flatMap(classroom -> {
                    String chatId = String.valueOf(classroom.getChatId());
                    Mono<List<AssignmentInfo>> assignmentsMono = assignmentRepo.findByChatId(chatId).collectList();
                    Mono<List<UserChatInfo>> studentsMono = userChatInfoRepo.findClassroomStudents(chatId).collectList();
                    Mono<byte[]> reportBytesMono = generateReport(roomId, format);

                    return Mono.zip(assignmentsMono, studentsMono, reportBytesMono)
                            .flatMap(tuple -> {
                                List<AssignmentInfo> assignments = tuple.getT1();
                                List<UserChatInfo> students = tuple.getT2();
                                byte[] bytes = tuple.getT3();

                                String generatedAt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                        .withZone(ZoneId.systemDefault())
                                        .format(Instant.now());

                                String subject = "Classroom Report: " + classroom.getRoomName() + " (Room " + roomId + ")";
                                String ext = "excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format) ? "xlsx" : ("csv".equalsIgnoreCase(format) ? "csv" : "pdf");
                                String fileName = "Classroom_Report_" + roomId + "." + ext;

                                String htmlBody = """
                                    <html>
                                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                                        <div style="max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
                                            <div style="background-color: #4A90E2; color: white; padding: 20px; text-align: center;">
                                                <h2 style="margin: 0;">Classroom Assignment Report</h2>
                                                <p style="margin: 5px 0 0 0; opacity: 0.9;">%s</p>
                                            </div>
                                            <div style="padding: 20px;">
                                                <h3>📊 Classroom Summary & Metadata</h3>
                                                <table style="width: 100%%; border-collapse: collapse; margin-bottom: 20px;">
                                                    <tr style="background-color: #f9f9f9;">
                                                        <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Classroom Name:</td>
                                                        <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Room ID:</td>
                                                        <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                                                    </tr>
                                                    <tr style="background-color: #f9f9f9;">
                                                        <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Total Enrolled Students:</td>
                                                        <td style="padding: 8px; border: 1px solid #ddd;">%d</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Total Assignments Created:</td>
                                                        <td style="padding: 8px; border: 1px solid #ddd;">%d</td>
                                                    </tr>
                                                    <tr style="background-color: #f9f9f9;">
                                                        <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Generated Date/Time:</td>
                                                        <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Attachment Format:</td>
                                                        <td style="padding: 8px; border: 1px solid #ddd; text-transform: uppercase;">%s</td>
                                                    </tr>
                                                </table>
                                                <p>Please find attached the complete matrix document detailing individual student assignment statuses and grades.</p>
                                                <p style="font-size: 0.85em; color: #777;">Sent via LetsChat Classroom System.</p>
                                            </div>
                                        </div>
                                    </body>
                                    </html>
                                    """.formatted(
                                        classroom.getRoomName(),
                                        classroom.getRoomName(),
                                        classroom.getRoomId(),
                                        students.size(),
                                        assignments.size(),
                                        generatedAt,
                                        format.toUpperCase()
                                );

                                if (brevoMailService != null && brevoMailService.isConfigured()) {
                                    return brevoMailService.sendEmailWithAttachment(recipientEmail, subject, htmlBody, fileName, bytes);
                                }
                                System.out.println("[ClassRoomReportService] BREVO_API_KEY is not configured in environment. Skipping email report dispatch for room " + roomId);
                                return Mono.just(false);
                            });
                });
    }

    private byte[] buildPdfReport(ClassRoomInfo classroom, List<AssignmentInfo> assignments, List<UserChatInfo> students, Map<String, Map<Long, SubmissionInfo>> matrix) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD, Color.BLACK);
            com.lowagie.text.Font subtitleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.NORMAL, Color.DARK_GRAY);

            document.add(new Paragraph("Classroom Assignment Matrix Report", titleFont));
            document.add(new Paragraph("Classroom: " + classroom.getRoomName() + " (Room ID: " + classroom.getRoomId() + ")", subtitleFont));
            document.add(new Paragraph("Generated At: " + DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.now()), subtitleFont));
            document.add(new Paragraph(" "));

            int cols = 2 + assignments.size();
            PdfPTable table = new PdfPTable(cols);
            table.setWidthPercentage(100);

            table.addCell(new PdfPCell(new Paragraph("Student Name")));
            for (AssignmentInfo a : assignments) {
                table.addCell(new PdfPCell(new Paragraph(a.getTitle() != null ? a.getTitle() : "Assign #" + a.getAssignmentId())));
            }
            table.addCell(new PdfPCell(new Paragraph("Completion")));

            for (UserChatInfo student : students) {
                table.addCell(new PdfPCell(new Paragraph(student.getUserName() != null ? student.getUserName() : student.getUserId())));
                Map<Long, SubmissionInfo> userSubs = matrix.getOrDefault(student.getUserId(), Map.of());
                int completed = 0;

                for (AssignmentInfo a : assignments) {
                    SubmissionInfo sub = userSubs.get(a.getAssignmentId());
                    PdfPCell cell = new PdfPCell();
                    if (sub != null) {
                        completed++;
                        String text = "[YES]";
                        if (sub.getMarks() != null) text += " (" + sub.getMarks() + " pts)";
                        cell.setPhrase(new Paragraph(text));
                        cell.setBackgroundColor(new Color(220, 248, 198));
                    } else {
                        cell.setPhrase(new Paragraph("[NO]"));
                        cell.setBackgroundColor(new Color(255, 205, 210));
                    }
                    table.addCell(cell);
                }

                double pct = assignments.size() > 0 ? ((double) completed / assignments.size()) * 100.0 : 0.0;
                table.addCell(new PdfPCell(new Paragraph(String.format("%d/%d (%.0f%%)", completed, assignments.size(), pct))));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private byte[] buildExcelReport(ClassRoomInfo classroom, List<AssignmentInfo> assignments, List<UserChatInfo> students, Map<String, Map<Long, SubmissionInfo>> matrix) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Assignment Report");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            CellStyle greenStyle = workbook.createCellStyle();
            greenStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle redStyle = workbook.createCellStyle();
            redStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            Cell c0 = headerRow.createCell(0);
            c0.setCellValue("Student Name");
            c0.setCellStyle(headerStyle);

            int colIdx = 1;
            for (AssignmentInfo a : assignments) {
                Cell c = headerRow.createCell(colIdx++);
                c.setCellValue(a.getTitle() != null ? a.getTitle() : "Assign #" + a.getAssignmentId());
                c.setCellStyle(headerStyle);
            }

            Cell lastCell = headerRow.createCell(colIdx);
            lastCell.setCellValue("Completion Rate");
            lastCell.setCellStyle(headerStyle);

            int rowIdx = 1;
            for (UserChatInfo student : students) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(student.getUserName() != null ? student.getUserName() : student.getUserId());

                Map<Long, SubmissionInfo> userSubs = matrix.getOrDefault(student.getUserId(), Map.of());
                int completed = 0;
                int cIndex = 1;

                for (AssignmentInfo a : assignments) {
                    Cell cell = row.createCell(cIndex++);
                    SubmissionInfo sub = userSubs.get(a.getAssignmentId());
                    if (sub != null) {
                        completed++;
                        String val = "SUBMITTED";
                        if (sub.getMarks() != null) val += " (" + sub.getMarks() + " pts)";
                        cell.setCellValue(val);
                        cell.setCellStyle(greenStyle);
                    } else {
                        cell.setCellValue("MISSING");
                        cell.setCellStyle(redStyle);
                    }
                }

                double pct = assignments.size() > 0 ? ((double) completed / assignments.size()) * 100.0 : 0.0;
                row.createCell(cIndex).setCellValue(String.format("%d/%d (%.0f%%)", completed, assignments.size(), pct));
            }

            for (int i = 0; i <= colIdx; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private byte[] buildCsvReport(ClassRoomInfo classroom, List<AssignmentInfo> assignments, List<UserChatInfo> students, Map<String, Map<Long, SubmissionInfo>> matrix) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintWriter writer = new PrintWriter(out)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Student Name");
            for (AssignmentInfo a : assignments) {
                sb.append(",\"").append(a.getTitle() != null ? a.getTitle().replace("\"", "\"\"") : "Assign #" + a.getAssignmentId()).append("\"");
            }
            sb.append(",Completion Rate\n");
            writer.print(sb.toString());

            for (UserChatInfo student : students) {
                StringBuilder rowSb = new StringBuilder();
                rowSb.append("\"").append(student.getUserName() != null ? student.getUserName().replace("\"", "\"\"") : student.getUserId()).append("\"");

                Map<Long, SubmissionInfo> userSubs = matrix.getOrDefault(student.getUserId(), Map.of());
                int completed = 0;

                for (AssignmentInfo a : assignments) {
                    SubmissionInfo sub = userSubs.get(a.getAssignmentId());
                    if (sub != null) {
                        completed++;
                        rowSb.append(",\"SUBMITTED");
                        if (sub.getMarks() != null) rowSb.append(" (").append(sub.getMarks()).append(" pts)");
                        rowSb.append("\"");
                    } else {
                        rowSb.append(",\"MISSING\"");
                    }
                }

                double pct = assignments.size() > 0 ? ((double) completed / assignments.size()) * 100.0 : 0.0;
                rowSb.append(String.format(",\"%d/%d (%.0f%%)\"\n", completed, assignments.size(), pct));
                writer.print(rowSb.toString());
            }

            writer.flush();
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
