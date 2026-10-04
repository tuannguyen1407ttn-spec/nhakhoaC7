package com.c7smile.clinic.service;

import com.c7smile.clinic.entity.Appointment;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:tuannguyen1407ttn@gmail.com}")
    private String fromEmail;

    private static final String APP_NAME = "Nha Khoa C7 Smile Clinic";
    private static final String CLINIC_ADDRESS = "85/75 Phú Diễn, Bắc Từ Liêm, Hà Nội";
    private static final String CLINIC_HOTLINE = "0879.742.058";

    /**
     * Gửi email xác thực tài khoản đăng ký mới.
     * Chạy bất đồng bộ (async) để không block giao diện người dùng.
     */
    public void sendVerificationEmail(String recipientEmail, String recipientName, String token, String baseUrl) {
        CompletableFuture.runAsync(() -> {
            try {
                if (mailSender == null) {
                    System.err.println("⚠️ JavaMailSender chưa được cấu hình. Bỏ qua gửi email.");
                    return;
                }

                String verifyUrl = baseUrl + "/verify-email?token=" + token;
                String subject = "[C7 Smile Clinic] Kích hoạt tài khoản của bạn";

                String htmlContent = "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.05);\">"
                        + "<div style=\"background: linear-gradient(135deg, #075e4a 0%, #0d9b7a 100%); padding: 36px 30px; text-align: center; color: #ffffff;\">"
                        + "<h1 style=\"margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;\">🦷 " + APP_NAME + "</h1>"
                        + "<p style=\"margin: 8px 0 0; font-size: 14px; opacity: 0.9;\">Hệ thống nha khoa kỹ thuật số tiêu chuẩn quốc tế</p>"
                        + "</div>"
                        + "<div style=\"padding: 36px 30px; color: #2d3748; line-height: 1.7;\">"
                        + "<h2 style=\"color: #0f172a; font-size: 20px; margin-top: 0;\">Xin chào " + (recipientName != null ? recipientName : "Quý khách") + ",</h2>"
                        + "<p>Cảm ơn bạn đã đăng ký tài khoản tại <strong>" + APP_NAME + "</strong>. Để đảm bảo an toàn thông tin và kích hoạt đầy đủ quyền lợi thành viên, vui lòng xác thực địa chỉ email của bạn.</p>"
                        + "<div style=\"text-align: center; margin: 35px 0;\">"
                        + "<a href=\"" + verifyUrl + "\" style=\"display: inline-block; background: #0d9b7a; color: #ffffff; text-decoration: none; padding: 14px 34px; border-radius: 10px; font-weight: 700; font-size: 16px; box-shadow: 0 4px 15px rgba(13,155,122,0.35);\">Kích Hoạt Tài Khoản Ngay</a>"
                        + "</div>"
                        + "<p style=\"font-size: 13px; color: #64748b;\">Nếu nút bấm trên không hoạt động, bạn có thể copy và dán liên kết sau vào trình duyệt:<br>"
                        + "<a href=\"" + verifyUrl + "\" style=\"color: #0d9b7a; word-break: break-all;\">" + verifyUrl + "</a></p>"
                        + "<div style=\"margin-top: 25px; padding: 14px; background: #f8fafc; border-left: 4px solid #0d9b7a; border-radius: 6px; font-size: 13px; color: #475569;\">"
                        + "<strong>Lưu ý:</strong> Liên kết xác thực này có hiệu lực trong vòng <strong>24 giờ</strong>. Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email."
                        + "</div>"
                        + "</div>"
                        + "<div style=\"background: #f1f5f9; padding: 20px 30px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0;\">"
                        + "<p style=\"margin: 0 0 6px;\">📍 Địa chỉ: " + CLINIC_ADDRESS + " | 📞 Hotline: " + CLINIC_HOTLINE + "</p>"
                        + "<p style=\"margin: 0;\">&copy; 2026 " + APP_NAME + ". Tất cả các quyền được bảo lưu.</p>"
                        + "</div>"
                        + "</div>";

                sendHtmlEmail(recipientEmail, subject, htmlContent);
                System.out.println("✅ Đã gửi email xác thực tài khoản tới: " + recipientEmail);
            } catch (Exception e) {
                System.err.println("❌ Lỗi khi gửi email xác thực tới " + recipientEmail + ": " + e.getMessage());
            }
        });
    }

    /**
     * Gửi email thông báo đặt lịch khám thành công.
     */
    public void sendBookingConfirmationEmail(String recipientEmail, Appointment appointment) {
        CompletableFuture.runAsync(() -> {
            try {
                if (mailSender == null) {
                    System.err.println("⚠️ JavaMailSender chưa được cấu hình. Bỏ qua gửi email.");
                    return;
                }

                String patientName = appointment.getPatient() != null ? appointment.getPatient().getFullName() : "Quý khách";
                String patientPhone = appointment.getPatient() != null ? appointment.getPatient().getPhone() : "Chưa cập nhật";
                String serviceName = appointment.getService() != null ? appointment.getService().getName() : "Khám tổng quát nha khoa";
                String appointmentDate = appointment.getAppointmentDate() != null 
                        ? appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                        : "Sẽ xác nhận";
                String timeSlot = appointment.getTimeSlot() != null ? appointment.getTimeSlot() : "Giờ hành chính";
                String notes = (appointment.getNotes() != null && !appointment.getNotes().trim().isEmpty()) 
                        ? appointment.getNotes() 
                        : "Không có ghi chú thêm";

                String subject = "[C7 Smile Clinic] Xác nhận đặt lịch khám thành công - Mã hẹn #" + appointment.getId();

                String htmlContent = "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.05);\">"
                        + "<div style=\"background: linear-gradient(135deg, #075e4a 0%, #0d9b7a 100%); padding: 32px 30px; text-align: center; color: #ffffff;\">"
                        + "<h1 style=\"margin: 0; font-size: 24px; font-weight: 800;\">🎉 ĐẶT LỊCH KHÁM THÀNH CÔNG</h1>"
                        + "<p style=\"margin: 6px 0 0; font-size: 14px; opacity: 0.9;\">" + APP_NAME + " trân trọng cảm ơn Quý khách!</p>"
                        + "</div>"
                        + "<div style=\"padding: 32px 30px; color: #2d3748; line-height: 1.6;\">"
                        + "<p style=\"margin-top: 0; font-size: 15px;\">Kính chào <strong>" + patientName + "</strong>,</p>"
                        + "<p>Yêu cầu đặt lịch khám nha khoa của Quý khách đã được hệ thống tiếp nhận thành công. Dưới đây là thông tin chi tiết:</p>"
                        + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin: 24px 0;\">"
                        + "<table style=\"width: 100%; border-collapse: collapse; font-size: 14px;\">"
                        + "<tr><td style=\"padding: 8px 0; color: #64748b; width: 140px;\">Mã lịch hẹn:</td><td style=\"padding: 8px 0; font-weight: 700; color: #0d9b7a;\">#" + appointment.getId() + "</td></tr>"
                        + "<tr><td style=\"padding: 8px 0; color: #64748b;\">Bệnh nhân:</td><td style=\"padding: 8px 0; font-weight: 600; color: #1e293b;\">" + patientName + "</td></tr>"
                        + "<tr><td style=\"padding: 8px 0; color: #64748b;\">Số điện thoại:</td><td style=\"padding: 8px 0; color: #1e293b;\">" + patientPhone + "</td></tr>"
                        + "<tr><td style=\"padding: 8px 0; color: #64748b;\">Dịch vụ:</td><td style=\"padding: 8px 0; font-weight: 600; color: #0f172a;\">" + serviceName + "</td></tr>"
                        + "<tr><td style=\"padding: 8px 0; color: #64748b;\">Ngày khám:</td><td style=\"padding: 8px 0; font-weight: 700; color: #e67e22;\">📅 " + appointmentDate + "</td></tr>"
                        + "<tr><td style=\"padding: 8px 0; color: #64748b;\">Khung giờ:</td><td style=\"padding: 8px 0; font-weight: 700; color: #e67e22;\">⏰ " + timeSlot + "</td></tr>"
                        + "<tr><td style=\"padding: 8px 0; color: #64748b;\">Ghi chú:</td><td style=\"padding: 8px 0; color: #64748b;\">" + notes + "</td></tr>"
                        + "<tr><td style=\"padding: 8px 0; color: #64748b;\">Trạng thái:</td><td style=\"padding: 8px 0;\"><span style=\"background: #fef3c7; color: #b45309; padding: 3px 10px; border-radius: 20px; font-size: 12px; font-weight: 600;\">Đang chờ xác nhận</span></td></tr>"
                        + "</table>"
                        + "</div>"
                        + "<div style=\"background: #ecfdf5; border-left: 4px solid #10b981; border-radius: 6px; padding: 14px; margin-bottom: 20px; font-size: 13px; color: #065f46;\">"
                        + "💡 <strong>Lưu ý:</strong> Đội ngũ lễ tân C7 Smile Clinic sẽ sớm liên hệ qua số điện thoại <strong>" + patientPhone + "</strong> để xác nhận lại lịch hẹn. Quý khách vui lòng đến trước giờ hẹn 10 phút để được phục vụ chu đáo nhất."
                        + "</div>"
                        + "<p style=\"font-size: 14px; color: #475569;\">Nếu cần thay đổi giờ hẹn hoặc cần hỗ trợ khẩn cấp, vui lòng liên hệ ngay hotline: <strong style=\"color: #0d9b7a;\">" + CLINIC_HOTLINE + "</strong>.</p>"
                        + "</div>"
                        + "<div style=\"background: #f1f5f9; padding: 20px 30px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0;\">"
                        + "<p style=\"margin: 0 0 6px;\">📍 Địa chỉ: " + CLINIC_ADDRESS + " | 📞 Hotline: " + CLINIC_HOTLINE + "</p>"
                        + "<p style=\"margin: 0;\">&copy; 2026 " + APP_NAME + ". Tất cả các quyền được bảo lưu.</p>"
                        + "</div>"
                        + "</div>";

                sendHtmlEmail(recipientEmail, subject, htmlContent);
                System.out.println("✅ Đã gửi email thông báo đặt lịch hẹn #" + appointment.getId() + " tới: " + recipientEmail);
            } catch (Exception e) {
                System.err.println("❌ Lỗi khi gửi email thông báo đặt lịch tới " + recipientEmail + ": " + e.getMessage());
            }
        });
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail, APP_NAME);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}
