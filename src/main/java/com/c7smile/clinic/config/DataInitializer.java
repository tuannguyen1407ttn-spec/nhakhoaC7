package com.c7smile.clinic.config;

import com.c7smile.clinic.entity.Dentist;
import com.c7smile.clinic.entity.Invoice;
import com.c7smile.clinic.entity.Service;
import com.c7smile.clinic.entity.User;
import com.c7smile.clinic.repository.DentistRepository;
import com.c7smile.clinic.repository.InvoiceRepository;
import com.c7smile.clinic.repository.ServiceRepository;
import com.c7smile.clinic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private DentistRepository dentistRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Override
    public void run(String... args) {
        migrateExistingUsers();
        initAdminUser();
        initReceptionUser();
        initDoctorUser();
        cleanLegacyDoctorIfSafe();
        enrichLegacyServicesIfAny();
        seedTenDentists();
        syncDoctorAccountsForDentists();
        seedRichServices();
        migrateInvoicesToPaid();
    }

    private void cleanLegacyDoctorIfSafe() {
        List<Dentist> dentists = dentistRepository.findAll();
        for (Dentist d : dentists) {
            if (d.getTraining() == null || d.getTraining().trim().isEmpty()) {
                try {
                    dentistRepository.delete(d);
                    System.out.println("🧹 Safely removed legacy test dentist: " + d.getFullName());
                } catch (Exception e) {
                    System.out.println("⚠️ Could not delete dentist " + d.getFullName() + ": " + e.getMessage());
                }
            }
        }
    }

    private void enrichLegacyServicesIfAny() {
        upsertService(
                "Khám Tổng Quát",
                100000.0,
                "30 - 45 phút",
                "https://images.unsplash.com/photo-1629909615184-74f495363b67?auto=format&fit=crop&w=800&q=80",
                "Khám và chẩn đoán tổng thể sức khỏe răng miệng bằng hệ thống máy quét 3D và chụp phim CT ConeBeam hiện đại.",
                "Dịch vụ Khám Tổng Quát tại C7 Smile Clinic là bước khởi đầu quan trọng nhất để bảo vệ hàm răng khỏe đẹp suốt đời. Toàn bộ quy trình thăm khám được thực hiện trực tiếp bởi đội ngũ bác sĩ chuyên khoa giàu kinh nghiệm với sự hỗ trợ của trang thiết bị công nghệ số hóa tiên tiến nhất.\n\nKhách hàng sẽ được chụp phim X-quang kỹ thuật số và quét camera nội soi độ phóng đại cao để phát hiện sớm các tổn thương men răng vi thể, sâu răng kẽ khuất, mầm răng ngầm, nang chân răng hoặc các dấu hiệu thoái hóa khớp thái dương hàm mà mắt thường không thể quan sát được.",
                "• Bước 1: Tiếp đón chu đáo và tạo lập bệnh án điện tử cá nhân hóa bảo mật.\n• Bước 2: Chụp phim X-quang toàn cảnh Panorama và CT ConeBeam 3D đa chiều.\n• Bước 3: Bác sĩ trực tiếp thăm khám răng, lợi, niêm mạc miệng và khớp cắn bằng camera phóng đại.\n• Bước 4: Đọc kết quả phân tích hình ảnh, giải thích chi tiết tình trạng sức khỏe răng miệng cho khách hàng.\n• Bước 5: Đưa ra phác đồ điều trị tối ưu (nếu có bệnh lý) và báo giá minh bạch trước khi thực hiện.\n• Bước 6: Hướng dẫn chăm sóc răng miệng khoa học tại nhà và hẹn lịch tái khám định kỳ.",
                "• Phát hiện sớm 100% các ổ sâu răng tiềm ẩn trước khi chúng ăn sâu vào tủy gây đau nhức.\n• Tiết kiệm đến 80% chi phí điều trị so với việc can thiệp khi bệnh lý đã trở nặng.\n• Được tư vấn cặn kẽ về khớp cắn và thẩm mỹ nụ cười chuẩn tỷ lệ vàng.\n• Nhận sổ theo dõi sức khỏe răng miệng điện tử trọn đời tại C7 Smile.",
                "• Dành cho mọi đối tượng từ trẻ nhỏ 3 tuổi đến người cao tuổi.\n• Người có triệu chứng ê buốt răng khi ăn đồ nóng lạnh, chảy máu chân răng khi đánh răng.\n• Người có nhu cầu kiểm tra định kỳ 6 tháng/lần theo khuyến cáo nha khoa quốc tế.",
                "• Đánh răng sạch sẽ trước khi đến phòng khám.\n• Mang theo hồ sơ bệnh án hoặc các toa thuốc đang sử dụng nếu có bệnh nền.",
                "• Duy trì thói quen chải răng 2 lần/ngày kết hợp chỉ nha khoa và tăm nước.\n• Hạn chế ăn đồ ngọt, nước ngọt có ga vào ban đêm.\n• Đặt lịch tái khám định kỳ mỗi 6 tháng một lần.");
    }

    private void migrateInvoicesToPaid() {
        List<Invoice> invoices = invoiceRepository.findAll();
        for (Invoice inv : invoices) {
            if (!"PAID".equals(inv.getStatus())) {
                inv.setStatus("PAID");
                inv.setPaidAmount(inv.getTotalAmount());
                invoiceRepository.save(inv);
                System.out.println("⚙️ Updated Invoice #" + inv.getId() + " to PAID");
            }
        }
    }

    private void migrateExistingUsers() {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole("CUSTOMER");
                userRepository.save(user);
                System.out.println("⚙️ Migrated user role: " + user.getUsername() + " → CUSTOMER");
            }
        }
    }

    private void initAdminUser() {
        if (userRepository.findByUsername("admin@c7smile.com") == null) {
            User admin = new User();
            admin.setUsername("admin@c7smile.com");
            admin.setFullName("Admin C7 Smile");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setPhone("0879742058");
            userRepository.save(admin);
            System.out.println("✅ Admin user created: admin@c7smile.com / admin123");
        }
    }

    private void initReceptionUser() {
        if (userRepository.findByUsername("letan@c7smile.com") == null) {
            User reception = new User();
            reception.setUsername("letan@c7smile.com");
            reception.setFullName("Lễ Tân C7 Smile");
            reception.setPassword(passwordEncoder.encode("letan123"));
            reception.setRole("RECEPTION");
            reception.setPhone("0123456789");
            userRepository.save(reception);
            System.out.println("✅ Reception user created: letan@c7smile.com / letan123");
        }
    }

    private void initDoctorUser() {
        if (userRepository.findByUsername("bacsi@c7smile.com") == null) {
            User doctor = new User();
            doctor.setUsername("bacsi@c7smile.com");
            doctor.setFullName("TS. BS. Nguyễn Văn Đức");
            doctor.setPassword(passwordEncoder.encode("bacsi123"));
            doctor.setRole("DOCTOR");
            doctor.setPhone("0987654321");
            userRepository.save(doctor);
            System.out.println("✅ Doctor user created: bacsi@c7smile.com / bacsi123");
        }
    }

    /**
     * Tự động sinh hoặc đồng bộ tài khoản người dùng cho toàn bộ bác sĩ trong CSDL.
     * Tên đăng nhập = email, Mật khẩu = 123456, Quyền = DOCTOR.
     */
    public void syncDoctorAccountsForDentists() {
        List<Dentist> dentists = dentistRepository.findAll();
        for (Dentist d : dentists) {
            if (d.getEmail() != null && !d.getEmail().trim().isEmpty()) {
                String email = d.getEmail().trim().toLowerCase();
                User user = userRepository.findByUsername(email);
                if (user == null) {
                    user = new User();
                    user.setUsername(email);
                }
                user.setPassword(passwordEncoder.encode("123456"));
                user.setRole("DOCTOR");
                user.setEnabled(true);
                user.setFullName(d.getFullName());
                if (d.getPhone() != null && !d.getPhone().trim().isEmpty()) {
                    user.setPhone(d.getPhone().trim());
                }
                if (d.getDob() != null) {
                    user.setDob(d.getDob());
                }
                userRepository.save(user);
                System.out.println("👨‍⚕️ Tự động đồng bộ tài khoản Bác sĩ: " + email + " / 123456 (Role: DOCTOR)");
            }
        }
    }

    /**
     * Khởi tạo hoặc cập nhật 10 Bác Sĩ hàng đầu với thông tin hồ sơ chuyên sâu
     */
    private void seedTenDentists() {
        if (dentistRepository.count() >= 10) {
            System.out.println("✅ 10 Dentists already populated (" + dentistRepository.count() + " doctors)");
            return;
        }

        System.out.println("🚀 Seeding 10 expert dentists into C7 Smile database...");

        // 1. TS. BS. Nguyễn Văn Đức
        upsertDentist(
                "TS. BS. Nguyễn Văn Đức",
                "0987654321",
                "duc.nguyen@c7smile.com",
                "001085002134",
                LocalDate.of(1978, 5, 12),
                "Giám Đốc Chuyên Môn - Chuyên Gia Cấy Ghép Implant & Phẫu Thuật Hàm Mặt",
                "Hơn 20 năm kinh nghiệm chuyên sâu phẫu thuật hàm mặt và cấy ghép Implant kỹ thuật cao.",
                "Cấy ghép Implant All-on-4 / All-on-6, Tái tạo xương vi phẫu, Nâng xoang hàm kín, Phẫu thuật chỉnh hình hàm mặt.",
                "• Tốt nghiệp Thủ khoa Bác sĩ Răng Hàm Mặt - Đại học Y Hà Nội (2001)\n" +
                        "• Bác sĩ Nội trú chuyên ngành Phẫu thuật Hàm mặt - BV Răng Hàm Mặt TW (2004)\n" +
                        "• Tiến sĩ Y khoa chuyên ngành Răng Hàm Mặt - Đại học Bordeaux (Cộng hòa Pháp, 2012)\n" +
                        "• Tu nghiệp chuyên sâu Phẫu thuật Tái tạo Xương & Ghép xương hàm tại ĐH Loma Linda (Hoa Kỳ)\n"
                        +
                        "• Chứng chỉ Cấy ghép Implant Toàn hàm Kỹ thuật số - Viện Malo Dental (Bồ Đào Nha)",
                "• 2004 - 2014: Bác sĩ Phẫu thuật Hàm mặt tại Bệnh viện Răng Hàm Mặt Trung Ương\n" +
                        "• 2014 - 2020: Trưởng khoa Cấy ghép Implant tại Bệnh viện Đa khoa Quốc tế Vinmec\n" +
                        "• 2020 - Hiện tại: Giám đốc Chuyên môn kiêm Cố vấn Phẫu thuật Cấp cao tại C7 Smile Clinic\n" +
                        "• Giảng viên thỉnh giảng bộ môn Phẫu thuật Miệng - ĐH Y Hà Nội",
                "• Thành viên chính thức Hiệp hội Cấy ghép Nha khoa Thụy Sĩ (ITI - International Team for Implantology)\n"
                        +
                        "• Chứng chỉ Diplomate Hiệp hội Cấy ghép Nha khoa Quốc tế (ICOI)\n" +
                        "• Chứng chỉ Phẫu thuật Cấy ghép Zygoma Implant không xương hàm - Straumann Academy\n" +
                        "• Chứng nhận Bác sĩ Xuất sắc Toàn cầu năm 2022 do Nobel Biocare trao tặng",
                "• Đã thực hiện thành công hơn 6.500 ca cấy ghép Implant đơn lẻ và phục hình toàn hàm phức tạp\n" +
                        "• Báo cáo viên danh dự tại các Hội nghị Nha khoa Quốc tế VIDEC (2020 - 2025)\n" +
                        "• Tác giả của 18 công trình nghiên cứu khoa học đăng trên các tạp chí y khoa quốc tế PubMed\n"
                        +
                        "• Huân chương Thầy thuốc Ưu tú vì sự nghiệp bảo vệ sức khỏe nhân dân",
                "Với hơn hai thập kỷ tận tụy cống hiến cho ngành nha khoa phục hình và phẫu thuật miệng, TS.BS Nguyễn Văn Đức luôn tâm niệm: 'Một nụ cười khỏe đẹp không chỉ đơn thuần là chức năng ăn nhai vững chắc, mà còn là khởi nguồn của sự tự tin và niềm hạnh phúc trong cuộc sống.' Bác sĩ luôn là người tiên phong đưa các công nghệ nha khoa số hóa hạn chế xâm lấn từ Châu Âu về Việt Nam, đem đến cho khách hàng trải nghiệm điều trị an toàn tuyệt đối và nhẹ nhàng nhất.",
                "https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&w=600&q=80");

        // 2. ThS. BS. Trần Thị Mai Anh
        upsertDentist(
                "ThS. BS. Trần Thị Mai Anh",
                "0912345678",
                "maianh.tran@c7smile.com",
                "001188003456",
                LocalDate.of(1983, 9, 24),
                "Trưởng Khoa Chỉnh Nha & Thẩm Mỹ Nụ Cười - Chuyên Gia Invisalign Platinum Elite",
                "Hơn 15 năm kinh nghiệm chuyên sâu nắn chỉnh răng công nghệ cao và kiến tạo khớp cắn chuẩn sinh học.",
                "Chỉnh nha khay trong suốt Invisalign, Mắc cài thông minh Damon, Nắn chỉnh răng lệch lạc phức tạp, Tiền chỉnh nha trẻ em.",
                "• Tốt nghiệp Bác sĩ Răng Hàm Mặt Xuất sắc - Đại học Y Dược TP.HCM (2006)\n" +
                        "• Thạc sĩ Chỉnh nha Thẩm mỹ - Seoul National University (Hàn Quốc, 2011)\n" +
                        "• Tốt nghiệp Khóa Đào tạo Chỉnh nha Cơ sinh học Biomechanics - Ormco Dental (Hoa Kỳ)\n" +
                        "• Chứng chỉ Chuyên gia Chỉnh nha Khay trong suốt Invisalign Toàn cầu (Hoa Kỳ)",
                "• 2006 - 2012: Bác sĩ Chỉnh nha tại Bệnh viện Răng Hàm Mặt TP.HCM\n" +
                        "• 2012 - 2019: Chuyên gia Chỉnh nha cao cấp tại Hệ thống Nha khoa Quốc tế Peace Dentistry\n" +
                        "• 2019 - Hiện tại: Trưởng khoa Chỉnh nha & Thẩm mỹ Nụ cười tại C7 Smile Clinic",
                "• Chứng nhận Invisalign Platinum Elite Provider (Align Technology, Hoa Kỳ)\n" +
                        "• Thành viên Hiệp hội Bác sĩ Chỉnh nha Thế giới (WFO - World Federation of Orthodontists)\n" +
                        "• Thành viên Hiệp hội Chỉnh nha Hoa Kỳ (AAO - American Association of Orthodontists)\n" +
                        "• Chứng chỉ Nắn chỉnh răng tăng trưởng trẻ em Myobrace & EF Line (Australia)",
                "• Đã kiến tạo nụ cười rạng rỡ cho hơn 4.200 ca khách hàng bằng niềng răng trong suốt và mắc cài\n" +
                        "• Top 5 Bác sĩ có số ca điều trị Invisalign thành công nhiều nhất miền Bắc năm 2023 - 2024\n" +
                        "• Khách mời chuyên gia tư vấn nha khoa thẩm mỹ trên các kênh truyền hình VTV, HTV",
                "ThS.BS Trần Thị Mai Anh được mệnh danh là 'phù thủy nụ cười' nhờ đôi bàn tay khéo léo, sự nhạy bén thẩm mỹ và sự ân cần thấu cảm. Triết lý điều trị của bác sĩ là: 'Bảo tồn tối đa răng thật nguyên bản, hạn chế tối đa việc nhổ răng và hướng tới việc tạo lập một khuôn mặt cân đối, góc nghiêng thần thánh chuẩn nhân trắc học.'",
                "https://images.unsplash.com/photo-1594824813501-489cae193309?auto=format&fit=crop&w=600&q=80");

        // 3. BSCKII. Lê Hoàng Nam
        upsertDentist(
                "BSCKII. Lê Hoàng Nam",
                "0934567890",
                "nam.le@c7smile.com",
                "001081005678",
                LocalDate.of(1980, 3, 15),
                "Chuyên Gia Phục Hình Răng Sứ & Thiết Kế Nụ Cười Kỹ Thuật Số (Digital Smile Design)",
                "18 năm kinh nghiệm chuyên sâu trong lĩnh vực dán sứ Veneer siêu mỏng và phục hình nụ cười quý tộc.",
                "Dán sứ Veneer siêu mỏng 0.2mm, Phục hình toàn sứ Emax/Cercon HT, Tái tạo khớp cắn toàn hàm, Thiết kế nụ cười DSD.",
                "• Bác sĩ Chuyên khoa II Phục hình Răng Thẩm mỹ - Đại học Y Hà Nội\n" +
                        "• Tu nghiệp chuyên sâu Phục hình Thẩm mỹ Bảo tồn Răng tại Đại học Zurich (Thụy Sĩ)\n" +
                        "• Hoàn thành chương trình Master Digital Smile Design (DSD) chứng nhận bởi Christian Coachman (Tây Ban Nha)\n"
                        +
                        "• Chứng chỉ Phục hình Răng sứ Thẩm mỹ Châu Âu (ESCD)",
                "• 2005 - 2016: Phó Trưởng khoa Phục hình Thẩm mỹ - Bệnh viện Đa khoa Quốc tế Thu Cúc\n" +
                        "• 2016 - 2021: Giám đốc Trung tâm Thẩm mỹ Nụ cười Kỹ thuật số Smile Design Center\n" +
                        "• 2021 - Hiện tại: Chuyên gia Phục hình Thẩm mỹ Trưởng tại C7 Smile Clinic",
                "• Chứng chỉ Giảng viên Master DSD Quốc tế\n" +
                        "• Chứng chỉ Master phục hình CAD/CAM Ceramill & Cerec của hãng Amann Girrbach (Đức)\n" +
                        "• Thành viên Hội Phục hình Thẩm mỹ Châu Âu (ESCD)",
                "• Đã thực hiện hơn 5.200 ca dán sứ Veneer và bọc răng sứ thẩm mỹ không đau, không viêm lợi\n" +
                        "• Cố vấn thẩm mỹ nụ cười cho hơn 100 hoa hậu, nghệ sĩ, diễn viên và người nổi tiếng tại Việt Nam\n"
                        +
                        "• Đạt giải thưởng Bàn tay vàng Nha khoa Thẩm mỹ Việt Nam năm 2021",
                "Với BSCKII Lê Hoàng Nam, một bộ răng đẹp không đơn thuần là trắng bóng mà phải có chiều sâu quang học, vân răng tinh tế và hòa quyện với phong thái, thần sắc của từng khách hàng. 'Tôi không làm răng giả, tôi kiến tạo những tác phẩm nghệ thuật sinh học bảo tồn trọn vẹn tủy và men răng tự nhiên của bạn.'",
                "https://images.unsplash.com/photo-1537368910025-700350fe46c7?auto=format&fit=crop&w=600&q=80");

        // 4. ThS. BS. Phạm Quốc Huy
        upsertDentist(
                "ThS. BS. Phạm Quốc Huy",
                "0945678901",
                "huy.pham@c7smile.com",
                "001087009876",
                LocalDate.of(1986, 11, 8),
                "Trưởng Khoa Phẫu Thuật Trong Miệng & Tiểu Phẫu Nhổ Răng Khôn Piezotome Không Đau",
                "12 năm kinh nghiệm phẫu thuật trong miệng, xử lý hơn 8.000 ca răng khôn mọc ngầm, mọc lệch phức tạp.",
                "Nhổ răng khôn sóng siêu âm Piezotome không đau, Phẫu thuật cắt chóp răng vi phẫu, Phẫu thuật tạo hình nướu thẩm mỹ.",
                "• Bác sĩ Răng Hàm Mặt - Đại học Y Hà Nội (2009)\n" +
                        "• Thạc sĩ Ngoại khoa Răng Hàm Mặt - Đại học Y Dược TP.HCM (2013)\n" +
                        "• Tu nghiệp Phẫu thuật Vi sóng Siêu âm Piezotome tại Frankfurt (CHLB Đức)\n" +
                        "• Chứng chỉ Cấp cứu Nha khoa Nâng cao (ACLS - American Heart Association)",
                "• 2010 - 2017: Bác sĩ Khoa Phẫu thuật Miệng & Hàm mặt - Bệnh viện Răng Hàm Mặt Trung Ương\n" +
                        "• 2017 - Hiện tại: Trưởng khoa Tiểu phẫu & Phẫu thuật trong miệng tại C7 Smile Clinic",
                "• Chứng chỉ Phẫu thuật Nhổ răng Không Sang chấn (Atraumatic Tooth Extraction)\n" +
                        "• Chứng nhận Sử dụng Công nghệ Gây tê Vi điểm Không đau DentalVibe (Hoa Kỳ)\n" +
                        "• Chứng chỉ Ghép màng huyết tương giàu tiểu cầu PRF kích thích liền thương thần tốc",
                "• Kỷ lục thực hiện ca nhổ răng khôn mọc ngang 90 độ sát dây thần kinh an toàn chỉ trong 6 phút\n" +
                        "• Hơn 8.000 bệnh nhân đánh giá 5 sao không sưng đau, hồi phục đi làm ngay ngày hôm sau\n" +
                        "• Tác giả cuốn cẩm nang 'Giải mã răng khôn - Những điều cần biết trước khi nhổ'",
                "ThS.BS Phạm Quốc Huy nổi tiếng với phong thái điềm đạm, ân cần và kỹ thuật nhổ răng 'êm như ru'. Bác sĩ chia sẻ: 'Nhiều người mang nỗi sợ hãi tột cùng mỗi khi nhắc đến nhổ răng khôn. Sứ mệnh của tôi là xua tan hoàn toàn nỗi sợ ấy bằng công nghệ sóng siêu âm êm ái và sự tận tâm chân thành nhất.'",
                "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?auto=format&fit=crop&w=600&q=80");

        // 5. BSCKI. Đỗ Thu Trang
        upsertDentist(
                "BSCKI. Đỗ Thu Trang",
                "0956789012",
                "trang.do@c7smile.com",
                "001189004567",
                LocalDate.of(1988, 7, 19),
                "Chuyên Gia Nha Khoa Trẻ Em & Chỉnh Hình Răng Tăng Trưởng Sớm",
                "10 năm kinh nghiệm điều trị nha khoa trẻ thơ, thấu hiểu tâm lý trẻ nhỏ và nắn chỉnh răng không sang chấn.",
                "Điều trị sâu răng trẻ em không đau, Hàn răng phòng ngừa men răng, Tiền chỉnh nha can thiệp sớm Myobrace, Chữa tủy răng sữa.",
                "• Bác sĩ Chuyên khoa I Răng Hàm Mặt - Đại học Y Hà Nội\n" +
                        "• Tu nghiệp Nha khoa Trẻ em & Tâm lý học Nhi khoa tại Tokyo Dental College (Nhật Bản)\n" +
                        "• Chứng chỉ Tiền chỉnh nha Tăng trưởng Hàm mặt Trẻ em EF Line (Pháp)",
                "• 2013 - 2019: Bác sĩ Khoa Răng Trẻ em - Bệnh viện Nhi Trung Ương\n" +
                        "• 2019 - Hiện tại: Trưởng bộ phận Nha khoa Nhi & Tiền Chỉnh nha tại C7 Smile Clinic",
                "• Chứng chỉ Điều trị Nha khoa Trẻ em Không Sang Chấn (ART)\n" +
                        "• Chứng nhận Ứng dụng Khí cười Nitrous Oxide giảm lo âu nha khoa ở trẻ nhỏ\n" +
                        "• Thành viên Hội Nha khoa Trẻ em Quốc tế (IAPD)",
                "• Đã khám và điều trị thành công cho hơn 7.000 bé nhỏ, giúp các bé hoàn toàn tự tin không sợ hãi nha sĩ\n"
                        +
                        "• Điều phối viên chiến dịch 'Nụ cười học đường' khám nha miễn phí cho hơn 15.000 học sinh tiểu học",
                "Với giọng nói dịu dàng, nụ cười ấm áp và sự kiên nhẫn vô tận, BSCKI Đỗ Thu Trang luôn biến mỗi buổi khám răng của các bạn nhỏ thành một chuyến phiêu lưu dũng cảm bảo vệ 'vương quốc răng xinh'.",
                "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=600&q=80");

        // 6. ThS. BS. Vũ Đình Khoa
        upsertDentist(
                "ThS. BS. Vũ Đình Khoa",
                "0967890123",
                "khoa.vu@c7smile.com",
                "001082006789",
                LocalDate.of(1982, 1, 30),
                "Chuyên Gia Cấy Ghép Implant Toàn Hàm All-on-4 / All-on-6",
                "14 năm chuyên tâm phục hồi chức năng nhai cho người trung niên và cao tuổi mất răng lâu năm tiêu xương nặng.",
                "Cấy ghép Implant tức thì, Phục hình bắt vít trên Implant, Ghép xương tự thân, Nâng xoang hở phức tạp.",
                "• Bác sĩ Răng Hàm Mặt - Đại học Y Hà Nội (2005)\n" +
                        "• Thạc sĩ chuyên ngành Implant Nha khoa - Đại học Mahidol (Thái Lan, 2011)\n" +
                        "• Tu nghiệp kỹ thuật Cấy ghép Zygoma Implant (xương gò má) tại Hoa Kỳ",
                "• 2007 - 2017: Bác sĩ Phục hình Implant tại Bệnh viện Răng Hàm Mặt Trung Ương\n" +
                        "• 2017 - Hiện tại: Chuyên gia Trưởng Cấy ghép Implant Toàn hàm tại C7 Smile Clinic",
                "• Chứng chỉ Master Implant Toàn hàm của Viện Nobel Biocare (Thụy Điển)\n" +
                        "• Chứng nhận Phục hình Chịu lực Tức thì Immediate Loading trong 24 giờ\n" +
                        "• Thành viên Hiệp hội Cấy ghép Nha khoa Châu Á - Thái Bình Dương (APIA)",
                "• Đã thực hiện hơn 1.200 ca phục hình Implant All-on-4 và All-on-6 giúp các cô chú cao tuổi tìm lại niềm vui ăn nhai trọn vẹn\n"
                        +
                        "• Tỷ lệ tích hợp xương thành công đạt 99.8% trong suốt 14 năm hành nghề",
                "ThS.BS Vũ Đình Khoa thấu hiểu sâu sắc nỗi khổ ăn không ngon miệng, hóp má, già trước tuổi của những cô chú bị mất răng. Bác sĩ luôn nỗ lực tìm kiếm giải pháp cấy ghép bền vững, an toàn và tiết kiệm thời gian nhất cho người cao tuổi.",
                "https://images.unsplash.com/photo-1582750433449-648ed127bb54?auto=format&fit=crop&w=600&q=80");

        // 7. BSCKI. Hoàng Bích Ngọc
        upsertDentist(
                "BSCKI. Hoàng Bích Ngọc",
                "0978901234",
                "ngoc.hoang@c7smile.com",
                "001187007890",
                LocalDate.of(1987, 8, 14),
                "Trưởng Khoa Nội Nha Vi Phẫu & Nha Chu Chuyên Sâu",
                "11 năm kinh nghiệm bảo tồn răng thật, điều trị dứt điểm các ca viêm tủy răng và bệnh lý nướu răng phức tạp.",
                "Điều trị tủy răng bằng kính hiển vi vi phẫu Carl Zeiss, Điều trị lại tủy hỏng, Phẫu thuật nạo túi nha chu, Ghép mô liên kết nướu.",
                "• Bác sĩ Chuyên khoa I Nội nha - Đại học Y Dược TP.HCM\n" +
                        "• Đào tạo Nội nha Vi phẫu tiên tiến sử dụng Kính hiển vi Carl Zeiss tại Singapore\n" +
                        "• Chứng chỉ Điều trị Bệnh nha chu & Tái tạo mô mềm quanh răng (AAP Hoa Kỳ)",
                "• 2012 - 2018: Bác sĩ Khoa Nội nha - Bệnh viện Răng Hàm Mặt TP.HCM\n" +
                        "• 2018 - Hiện tại: Trưởng khoa Nội nha & Nha chu tại C7 Smile Clinic",
                "• Chứng chỉ Chuyên gia Nội nha Vi phẫu Quốc tế (Microscopic Endodontics Specialist)\n" +
                        "• Chứng chỉ Hệ thống Trâm quay WaveOne Gold và Bít tủy nhiệt 3D Gutta-percha\n" +
                        "• Thành viên Hội Nội nha Châu Á - Thái Bình Dương (APEC)",
                "• Đã giải cứu thành công hơn 3.800 chiếc răng bị nang chân răng, viêm tủy hoại tử ngỡ như phải nhổ bỏ\n"
                        +
                        "• Tỷ lệ chữa tủy sạch khuẩn và không tái phát đạt 99.4%",
                "Bảo tồn tối đa răng thật tự nhiên của người bệnh là kim chỉ nam cho mọi hành động của BSCKI Hoàng Bích Ngọc. Từng thao tác của bác sĩ đều chuẩn xác đến từng micromet dưới sự trợ lực của kính hiển vi quang học.",
                "https://images.unsplash.com/photo-1594824813681-367d3e4c0e66?auto=format&fit=crop&w=600&q=80");

        // 8. BSCKII. Đặng Minh Tuấn
        upsertDentist(
                "BSCKII. Đặng Minh Tuấn",
                "0989012345",
                "tuan.dang@c7smile.com",
                "001080008901",
                LocalDate.of(1981, 12, 5),
                "Chuyên Gia Khớp Thái Dương Hàm (TMJ) & Phục Hồi Khớp Cắn Toàn Diện",
                "17 năm kinh nghiệm chẩn đoán và điều trị rối loạn khớp thái dương hàm, nghiến răng và sai lệch khớp cắn nặng.",
                "Trị liệu rối loạn khớp thái dương hàm TMJ, Nẹp má khớp cắn, Mài chỉnh khớp cắn chọn lọc, Tái lập mặt phẳng nhai.",
                "• Bác sĩ Chuyên khoa II Răng Hàm Mặt - Đại học Y Hà Nội\n" +
                        "• Đào tạo Chuyên sâu Rối loạn Khớp cắn & Thần kinh Cơ học tại Vienna School of Dentistry (Cộng hòa Áo)\n"
                        +
                        "• Chứng chỉ Phân tích Chuyển động Hàm Kỹ thuật số Cadiax 4",
                "• 2006 - 2016: Bác sĩ Khoa Phục hình & Khớp cắn - Bệnh viện Bạch Mai\n" +
                        "• 2016 - Hiện tại: Chuyên gia Cố vấn Khớp cắn & TMJ tại C7 Smile Clinic",
                "• Thành viên Hiệp hội Khớp cắn Quốc tế (ICCMO)\n" +
                        "• Chứng chỉ Trị liệu Má nẹp khớp cắn giải áp đĩa đệm (Occlusal Splint Therapy)\n" +
                        "• Chứng nhận Điều trị Hội chứng Đau Rối loạn Cơ Khớp Hàm Mặt Châu Âu",
                "• Đã điều trị dứt điểm đau nhức khớp thái dương hàm, tiếng kêu lục cục và mỏi cơ hàm cho hơn 2.500 bệnh nhân\n"
                        +
                        "• Khôi phục chức năng nhai thăng bằng và giải phóng chứng đau đầu mãn tính do sai khớp cắn",
                "BSCKII Đặng Minh Tuấn tiếp cận nha khoa dưới góc nhìn toàn diện cơ - khớp - răng. Bác sĩ giúp người bệnh tìm lại nụ cười không chỉ đẹp về thẩm mỹ mà còn khỏe khoắn, thư giãn và trọn vẹn từ gốc rễ.",
                "https://images.unsplash.com/photo-1622902046580-2b47f47f5471?auto=format&fit=crop&w=600&q=80");

        // 9. ThS. BS. Bùi Phương Linh
        upsertDentist(
                "ThS. BS. Bùi Phương Linh",
                "0901234567",
                "linh.bui@c7smile.com",
                "001191001234",
                LocalDate.of(1990, 6, 18),
                "Chuyên Gia Thẩm Mỹ Nụ Cười & Tẩy Trắng Răng Công Nghệ Cao Laser Whitening",
                "9 năm kinh nghiệm chuyên sâu trong nha khoa thẩm mỹ không xâm lấn, đắp mặt sứ thẩm mỹ và tẩy trắng răng.",
                "Tẩy trắng răng Laser không ê buốt, Trám răng thẩm mỹ vi phẫu composite nghệ thuật, Đánh bóng men răng Airflow, Đính đá răng.",
                "• Bác sĩ Răng Hàm Mặt Xuất sắc - Đại học Y Hà Nội (2014)\n" +
                        "• Thạc sĩ Răng Hàm Mặt chuyên ngành Nha khoa Thẩm mỹ - Đại học Y Dược Huế\n" +
                        "• Đào tạo công nghệ Tẩy trắng răng Laser Philips Zoom WhiteSpeed tại Singapore",
                "• 2014 - 2019: Bác sĩ Thẩm mỹ Nha khoa tại Thẩm mỹ viện Quốc tế Kangnam\n" +
                        "• 2019 - Hiện tại: Bác sĩ Chuyên khoa Thẩm mỹ Nụ cười tại C7 Smile Clinic",
                "• Chứng chỉ Tẩy trắng răng An toàn Quốc tế Philips Zoom Advanced\n" +
                        "• Chứng chỉ Đắp mặt Composite Thẩm mỹ Trực tiếp StyleItaliano (Ý)\n" +
                        "• Thành viên Hội Nha sĩ Trẻ Châu Á",
                "• Thực hiện hơn 4.500 ca tẩy trắng răng Laser và tái tạo men răng thẩm mỹ rực rỡ\n" +
                        "• Chuyên gia tư vấn sắc màu men răng cho các studio ảnh cưới và các cuộc thi người mẫu ảnh",
                "Với sự tỉ mỉ, khéo léo và con mắt nghệ thuật tinh tế, ThS.BS Bùi Phương Linh luôn giúp mọi khách hàng sở hữu nụ cười tươi tắn, trắng sáng tự nhiên chỉ sau 45 phút thư giãn trên ghế nha khoa.",
                "https://images.unsplash.com/photo-1527613426441-4da17471b66d?auto=format&fit=crop&w=600&q=80");

        // 10. BSCKI. Phan Anh Dũng
        upsertDentist(
                "BSCKI. Phan Anh Dũng",
                "0912345670",
                "dung.phan@c7smile.com",
                "001092002345",
                LocalDate.of(1991, 10, 22),
                "Trưởng Phòng Chẩn Đoán Hình Ảnh Kỹ Thuật Số & Nha Khoa Tổng Quát",
                "8 năm kinh nghiệm vận hành hệ thống chẩn đoán hình ảnh kỹ thuật số 3D và khám nha khoa tổng quát chuẩn Bộ Y Tế.",
                "Chụp và phân tích phim CT ConeBeam 3D, Quét mẫu hàm 3D iTero Element 5D, Lấy cao răng siêu âm Piezo, Khám lập phác đồ toàn diện.",
                "• Bác sĩ Chuyên khoa I - Đại học Y Hà Nội (2015)\n" +
                        "• Chứng nhận Chuyên gia Chẩn đoán Hình ảnh CT ConeBeam 3D Nha khoa Quốc tế\n" +
                        "• Đào tạo Quét dấu hàm Kỹ thuật số iTero 5D & Trios 3Shape tại Hàn Quốc",
                "• 2015 - 2020: Bác sĩ Chẩn đoán Hình ảnh & Khám Tổng quát tại Bệnh viện Việt Đức\n" +
                        "• 2020 - Hiện tại: Trưởng phòng Chẩn đoán Kỹ thuật số tại C7 Smile Clinic",
                "• Chứng chỉ Đọc và Phân tích Phim X-quang Nha khoa Kỹ thuật số 3D\n" +
                        "• Chứng chỉ Kiểm soát Nhiễm khuẩn và Vô trùng Tuyệt đối Bệnh viện Nha khoa\n" +
                        "• Chứng chỉ Lấy cao răng và Chăm sóc Nướu Siêu âm Không Sang chấn",
                "• Đã thực hiện hơn 12.000 ca quét mẫu hàm và chẩn đoán cấu trúc giải phẫu xương răng chính xác 100%\n"
                        +
                        "• Thiết lập quy trình '1 Khách hàng - 1 Ghế nha - 1 Bộ dụng cụ vô trùng riêng biệt' chuẩn quốc tế tại C7 Smile Clinic",
                "BSCKI Phan Anh Dũng là người đặt nền móng chẩn đoán chính xác tuyệt đối cho mọi ca điều trị tại C7 Smile Clinic. Bác sĩ tin rằng: 'Chẩn đoán đúng là một nửa thành công của phác đồ điều trị. Mỗi dữ liệu hình ảnh 3D đều là chìa khóa bảo vệ nụ cười trọn đời của bạn.'",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=600&q=80");

        System.out.println("✅ Successfully seeded 10 prestigious dentists!");
    }

    private void upsertDentist(String fullName, String phone, String email, String cccd, LocalDate dob,
            String position, String shortDescription, String expertise,
            String training, String experience, String certificates,
            String achievements, String bio, String avatarUrl) {
        Dentist d = dentistRepository.findAll().stream()
                .filter(x -> x.getFullName().equalsIgnoreCase(fullName))
                .findFirst()
                .orElse(new Dentist());

        d.setFullName(fullName);
        d.setPhone(phone);
        d.setEmail(email);
        d.setCccd(cccd);
        d.setDob(dob);
        d.setPosition(position);
        d.setShortDescription(shortDescription);
        d.setExpertise(expertise);
        d.setTraining(training);
        d.setExperience(experience);
        d.setCertificates(certificates);
        d.setAchievements(achievements);
        d.setBio(bio);
        d.setAvatarUrl(avatarUrl);
        d.setStatus("Hoạt động");

        dentistRepository.save(d);
    }

    /**
     * Khởi tạo và cập nhật 8 dịch vụ cốt lõi với nội dung phong phú, siêu chi tiết
     * và hấp dẫn người dùng.
     */
    private void seedRichServices() {
        System.out.println("🚀 Seeding rich, long-form dental services...");

        // 1. Cấy Ghép Implant Thụy Sĩ
        upsertService(
                "Cấy Ghép Implant Thụy Sĩ Chuẩn Quốc Tế",
                15000000.0,
                "45 - 60 phút / trụ",
                "https://images.unsplash.com/photo-1606811841689-23dfddce3e95?auto=format&fit=crop&w=800&q=80",
                "Phục hồi răng mất hoàn hảo nhất hiện nay, tái sinh chân răng Titanium tương thích sinh học trọn đời, ngăn ngừa tiêu xương hàm 100%.",
                "Cấy ghép răng Implant tại C7 Smile Clinic là giải pháp phục hình răng đã mất tiên tiến bậc nhất thế giới, tái tạo hoàn chỉnh cấu trúc gồm cả chân răng (trụ Implant) và thân răng sứ phía trên.\n\n"
                        +
                        "Trụ Implant được chế tác từ 100% Titanium tinh khiết có khả năng tích hợp sinh học bền vững với tế bào xương hàm, đóng vai trò như một chân răng thật vững chắc. Dưới sự hỗ trợ của hệ thống định vị phẫu thuật số 3D Guide và máy chụp phim CT ConeBeam hiện đại, bác sĩ có thể tính toán chính xác từng góc nghiêng và độ sâu đến milimet, hoàn toàn không xâm lấn mô lành xung quanh, hạn chế tối đa cảm giác sưng đau hay chảy máu.\n\n"
                        +
                        "Sau khi trụ Implant tích hợp thành công, mão răng toàn sứ cao cấp (Cercon, Emax) được gắn cố định lên trên, đem lại khả năng ăn nhai chắc khỏe như răng thật lên đến 99% và vẻ đẹp tự nhiên khó phân biệt.",
                "• Bước 1: Thăm khám lâm sàng tổng quát & Chụp phim CT ConeBeam 3D đa chiều khảo sát mật độ xương hàm và hệ thống dây thần kinh.\n"
                        +
                        "• Bước 2: Phân tích dữ liệu bằng phần mềm giả lập cấy ghép 3D Implant Studio, lên phác đồ điều trị chi tiết và in máng hướng dẫn phẫu thuật cá nhân hóa.\n"
                        +
                        "• Bước 3: Vệ sinh sát khuẩn khoang miệng và thực hiện gây tê vi điểm không đau bằng công nghệ DentalVibe.\n"
                        +
                        "• Bước 4: Tiến hành cấy trụ Implant vào xương hàm trong phòng phẫu thuật áp lực âm vô trùng tuyệt đối chỉ trong 15-20 phút/trụ.\n"
                        +
                        "• Bước 5: Lắp răng tạm thẩm mỹ tức thì để đảm bảo tính thẩm mỹ nụ cười và hỗ trợ ăn nhai nhẹ trong thời gian chờ tích hợp xương.\n"
                        +
                        "• Bước 6: Tái khám kiểm tra độ tích hợp sinh học xương sau 6-12 tuần, lấy dấu hàm số hóa bằng máy quét 3D iTero Element 5D.\n"
                        +
                        "• Bước 7: Lắp mão toàn sứ vĩnh viễn, tinh chỉnh khớp cắn chuẩn xác và kích hoạt thẻ bảo hành chính hãng toàn cầu từ 10 năm đến trọn đời.",
                "• Khôi phục sức nhai hoàn hảo đạt 99% như răng tự nhiên, ăn nhai thoải mái không kiêng khem.\n" +
                        "• Ngăn chặn triệt để tình trạng tiêu xương ổ răng, tụt nướu, hóp má và biến dạng lão hóa khuôn mặt do mất răng gây ra.\n"
                        +
                        "• Bảo tồn tối đa răng thật kế cận: Tuyệt đối không cần mài nhỏ 2 răng bên cạnh như phương pháp làm cầu răng sứ truyền thống.\n"
                        +
                        "• Tương thích sinh học 100%, không bị oxy hóa hay đào thải trong môi trường khoang miệng.\n" +
                        "• Tuổi thọ vĩnh viễn: Nếu được chăm sóc vệ sinh đúng cách, trụ Implant có thể tồn tại bền vững trọn đời.\n"
                        +
                        "• Cấp thẻ bảo hành chính hãng quốc tế có mã QR tra cứu xuất xứ nguồn gốc rõ ràng.",
                "• Người bị mất 1 răng, nhiều răng hoặc mất toàn bộ răng hàm trên / hàm dưới.\n" +
                        "• Người đang sử dụng hàm giả tháo lắp bị lỏng lẻo, đau đớn, bất tiện trong ăn uống muốn chuyển sang răng cố định.\n"
                        +
                        "• Người bị hỏng cầu răng sứ cũ muốn phục hình lại bền vững.\n" +
                        "• Khách hàng từ 18 tuổi trở lên có sức khỏe ổn định và mật độ xương hàm phát triển hoàn thiện.",
                "• Khách hàng cần thông báo tiền sử bệnh lý tim mạch, tiểu đường, huyết áp hoặc phụ nữ đang mang thai.\n"
                        +
                        "• Tuyệt đối kiêng hút thuốc lá và uống bia rượu trước và sau phẫu thuật ít nhất 2 tuần để vết mổ tích hợp xương tối ưu.\n"
                        +
                        "• Uống thuốc kháng sinh, chống phù nề đúng liều lượng theo toa của bác sĩ chuyên khoa.",
                "• Chườm đá lạnh ngoài má ngắt quãng trong 24 giờ đầu để hạn chế tối đa sưng nề.\n" +
                        "• Ăn thức ăn mềm, ấm nguội như cháo, súp trong 3 ngày đầu, tránh nhai trực tiếp vào vị trí vừa cấy trụ.\n"
                        +
                        "• Sử dụng nước súc miệng chuyên dụng Chlorhexidine theo hướng dẫn, không dùng tăm xỉa vào huyệt ổ răng.\n"
                        +
                        "• Tái khám định kỳ theo lịch hẹn 1 tuần (cắt chỉ nếu có), 1 tháng, 3 tháng và 6 tháng.");

        // 2. Niềng Răng Trong Suốt Invisalign
        upsertService(
                "Niềng Răng Trong Suốt Invisalign Hoa Kỳ",
                45000000.0,
                "12 - 24 tháng (tùy tình trạng răng)",
                "https://images.unsplash.com/photo-1588776814546-1ffcf47267a5?auto=format&fit=crop&w=800&q=80",
                "Đỉnh cao chỉnh nha vô hình thế giới với vật liệu độc quyền SmartTrack, biết trước kết quả dịch chuyển răng qua mô phỏng 3D ClinCheck.",
                "Niềng răng trong suốt Invisalign là phương pháp nắn chỉnh răng tiên tiến nhất toàn cầu xuất xứ từ Tập đoàn Align Technology (Hoa Kỳ). Thay vì sử dụng hệ thống mắc cài và dây cung kim loại vướng víu, Invisalign sử dụng một chuỗi các khay niềng trong suốt bằng chất liệu nhựa sinh học SmartTrack được thiết kế cá nhân hóa cho từng bệnh nhân.\n\n"
                        +
                        "Khay niềng ôm sát từng đường viền cung răng, tạo lực dịch chuyển sinh học nhẹ nhàng, liên tục và êm ái để đưa răng về đúng vị trí chuẩn khớp cắn. Điểm đặc biệt nhất là công nghệ phần mềm 3D ClinCheck giúp khách hàng có thể nhìn thấy trước toàn bộ quá trình dịch chuyển của từng chiếc răng theo từng tuần và hình ảnh nụ cười hoàn mỹ sau khi kết thúc lộ trình.\n\n"
                        +
                        "Khách hàng có thể dễ dàng tự tháo khay khi ăn uống và vệ sinh răng miệng, hoàn toàn tự tin giao tiếp mà không ai nhận ra bạn đang niềng răng.",
                "• Bước 1: Khám tổng quát khớp cắn, chụp phim X-quang Panorama, Cephalo và quét mẫu hàm kỹ thuật số 3D iTero Element 5D chỉ trong 60 giây.\n"
                        +
                        "• Bước 2: Bác sĩ chuyên khoa lên phác đồ điều trị 3D ClinCheck chi tiết, mô phỏng từng giai đoạn di chuyển răng để khách hàng duyệt trước.\n"
                        +
                        "• Bước 3: Dữ liệu kỹ thuật số được gửi trực tiếp sang nhà máy Align Technology tại Hoa Kỳ để sản xuất trọn bộ khay niềng cá nhân hóa.\n"
                        +
                        "• Bước 4: Nhận khay niềng tại C7 Smile, bác sĩ hướng dẫn kỹ thuật tháo lắp khay, cách bảo quản và gắn các nút tạo lực Attachment thẩm mỹ.\n"
                        +
                        "• Bước 5: Khách hàng tự thay khay mới tại nhà sau mỗi 7 - 10 ngày, tái khám trực tiếp hoặc theo dõi từ xa qua ứng dụng nha khoa mỗi 6 - 8 tuần.\n"
                        +
                        "• Bước 6: Hoàn tất quá trình dịch chuyển răng, tinh chỉnh khớp cắn và đeo khay duy trì Vivera để nụ cười luôn chuẩn đẹp bền vững trọn đời.",
                "• Khay niềng trong suốt 100%: Gần như vô hình, mang lại sự tự tin tuyệt đối trong mọi giao tiếp hàng ngày.\n"
                        +
                        "• Tháo lắp linh hoạt: Thoải mái thưởng thức mọi món ăn yêu thích mà không phải kiêng khem khổ sở như mắc cài sắt.\n"
                        +
                        "• Vệ sinh răng miệng dễ dàng: Giảm thiểu 95% nguy cơ sâu răng, viêm nướu, hôi miệng trong suốt quá trình niềng.\n"
                        +
                        "• Không đau đớn, không cọ xát tổn thương nướu, má và môi.\n" +
                        "• Tiết kiệm thời gian tái khám: Rất thích hợp cho người bận rộn, doanh nhân, diễn viên hoặc du học sinh xa nhà.\n"
                        +
                        "• Xem trước kết quả điều trị qua video 3D ClinCheck sống động.",
                "• Răng bị hô, vẩu, móm, khớp cắn ngược, khớp cắn sâu hoặc cắn hở.\n" +
                        "• Răng mọc khấp khểnh, chen chúc, nghiêng ngả hoặc răng thưa hở kẽ.\n" +
                        "• Người làm công việc đòi hỏi tính thẩm mỹ cao: MC, diễn viên, người mẫu, tiếp viên hàng không, doanh nhân...\n"
                        +
                        "• Bệnh nhân từng niềng răng mắc cài trước đây nhưng bị chạy răng trở lại.",
                "• Cần tuân thủ đeo khay tối thiểu từ 20 đến 22 giờ mỗi ngày để đảm bảo lực dịch chuyển răng hiệu quả nhất.\n"
                        +
                        "• Tháo khay trước khi ăn uống (chỉ được uống nước lọc nguội khi đang đeo khay).\n" +
                        "• Vệ sinh khay bằng bàn chải lông mềm và nước mát, tránh dùng nước sôi gây biến dạng khay nhựa.",
                "• Luôn cất khay niềng vào hộp chuyên dụng cá nhân khi tháo ra ăn uống, tránh gói vào khăn giấy dễ thất lạc.\n"
                        +
                        "• Đánh răng và dùng chỉ nha khoa sạch sẽ trước khi đeo lại khay sau bữa ăn.\n" +
                        "• Tuân thủ đeo hàm duy trì Vivera theo đúng chỉ định sau khi tháo niềng để chống chạy răng.");

        // 3. Dán Sứ Veneer Siêu Mỏng
        upsertService(
                "Dán Sứ Veneer Siêu Mỏng Không Mài Nhỏ Răng",
                6500000.0,
                "2 buổi hẹn (cách nhau 2 - 3 ngày)",
                "https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=800&q=80",
                "Đỉnh cao phục hình nụ cười bảo tồn răng thật, chỉ mài siêu mịn bề mặt men 0.2 - 0.5mm, biến hàm răng ố xỉn thành nụ cười chuẩn minh tinh.",
                "Dán sứ Veneer là giải pháp làm đẹp nụ cười đẳng cấp nhất trong nha khoa hiện đại, được đông đảo nghệ sĩ, hoa hậu và doanh nhân trên thế giới ưa chuộng. Khác với phương pháp bọc răng sứ truyền thống phải mài nhỏ răng thật, dán sứ Veneer chỉ làm mịn siêu mỏng bề mặt ngoài của men răng từ 0.2 - 0.5mm (mỏng như kính áp tròng).\n\n"
                        +
                        "Mặt dán sứ được chế tác từ phôi sứ thủy tinh cao cấp IPS Emax Press (Đức/Thụy Sĩ) có độ trong mờ, rìa cắn và đường vân phản quang tự nhiên giống hệt răng thật. Sử dụng hệ thống keo dán sinh học nha khoa chuyên dụng tạo liên kết siêu bền chắc, miếng dán sứ ôm khít hoàn hảo vào men răng, không gây bong rơi, không hôi miệng, không ê buốt và bảo tồn tối đa 100% tủy răng sống.",
                "• Bước 1: Khám tổng quát cung hàm, chụp ảnh chân dung và thiết kế nụ cười số Digital Smile Design (DSD) cá nhân hóa theo tỷ lệ vàng khuôn mặt.\n"
                        +
                        "• Bước 2: Trải nghiệm công nghệ Mock-up đắp thử form răng trực tiếp trên miệng, khách hàng được nhìn và duyệt trước dáng răng ưng ý.\n"
                        +
                        "• Bước 3: Sát khuẩn, gây tê nhẹ và xử lý siêu mịn bề mặt men răng từ 0.2 - 0.5mm, quét dấu hàm kỹ thuật số 3D Trios/iTero.\n"
                        +
                        "• Bước 4: Chế tác mặt dán sứ thủ công tinh xảo bởi các nghệ nhân nha khoa kết hợp công nghệ tiện CAD/CAM tại Labo tiêu chuẩn Châu Âu.\n"
                        +
                        "• Bước 5: Thử mặt sứ trên răng, tinh chỉnh màu sắc hài hòa với nước da và cố định vĩnh viễn bằng keo quang trùng hợp cao cấp.\n"
                        +
                        "• Bước 6: Đánh bóng bề mặt, kiểm tra độ ăn khớp rìa cắn và cấp thẻ bảo hành chính hãng điện tử 15 năm.",
                "• Bảo tồn răng thật tối đa: Tuyệt đối không mài nhỏ răng thành cùi, không diệt tủy răng, giữ gìn răng khỏe mạnh nguyên bản.\n"
                        +
                        "• Màu sắc tự nhiên trong suốt: Khả năng khúc xạ ánh sáng hoàn mỹ, tạo vẻ đẹp quý phái, thanh thoát.\n"
                        +
                        "• Kháng màu thực phẩm tuyệt đối: Không bao giờ bám màu cà phê, trà, rượu vang đỏ hay thuốc lá.\n"
                        +
                        "• Ăn nhai thoải mái không ê buốt: Cảm giác nhai cắn thức ăn chân thực 100% như răng tự nhiên.\n"
                        +
                        "• Độ bền vượt trội từ 15 đến 20 năm, không gây kích ứng viền nướu.",
                "• Răng bị ố vàng, nhiễm màu kháng sinh Tetracycline nặng không thể tẩy trắng.\n" +
                        "• Răng có hình thể bất cân xứng, ngắn, mẻ vỡ góc hoặc mòn rìa cắn.\n" +
                        "• Răng thưa kẽ nhẹ, hở kẽ răng cửa làm mất tự tin khi cười.\n" +
                        "• Người mong muốn nâng tầm nụ cười sang trọng mà vẫn bảo vệ răng thật nguyên vẹn.",
                "• Tránh dùng răng cửa cắn trực tiếp vào các vật cứng như mở nắp chai, cắn xương, cắn hạt dưa cứng.\n" +
                        "• Khách hàng có thói quen nghiến răng ban đêm nên làm thêm máng chống nghiến để bảo vệ mặt dán sứ.\n"
                        +
                        "• Sử dụng chỉ nha khoa và tăm nước để vệ sinh kẽ răng hàng ngày.",
                "• Đánh răng 2-3 lần/ngày bằng bàn chải lông siêu mềm và kem đánh răng không có hạt mài mòn mạnh.\n" +
                        "• Tái khám định kỳ 6 tháng một lần để nha sĩ kiểm tra độ bám dính và đánh bóng men sứ sáng đẹp.");

        // 4. Bọc Răng Sứ Toàn Sứ Cao Cấp
        upsertService(
                "Bọc Răng Sứ Thẩm Mỹ Toàn Sứ Cercon HT & Zirconia",
                3500000.0,
                "2 - 3 ngày (2 buổi hẹn)",
                "https://images.unsplash.com/photo-1629909613654-28e377c37b09?auto=format&fit=crop&w=800&q=80",
                "Tái tạo hàm răng hư tổn, sâu gãy lớn hoặc xỉn màu với dòng sứ nguyên khối chịu lực gấp 7 lần răng thật, bảo hành chính hãng 15 năm.",
                "Bọc răng sứ thẩm mỹ tại C7 Smile Clinic sử dụng 100% phôi sứ nguyên khối cao cấp nhập khẩu chính hãng từ Đức và Mỹ như Cercon HT, Zirconia Katana, Lava Plus. Điểm khác biệt vượt trội so với răng sứ kim loại cũ là răng toàn sứ không chứa bất kỳ tạp chất kim loại nào, do đó hoàn toàn không gây đen viền nướu, không kích ứng mô mềm và có tính tương thích sinh học tuyệt đối.\n\n"
                        +
                        "Răng sứ được thiết kế và cắt tiện bằng hệ thống máy CAD/CAM hiện đại với độ khít sát đạt chuẩn micromet, nung kết ở nhiệt độ cao 1500 độ C đem lại khả năng chịu lực uốn gãy lên tới 1200 - 1500 MPa (gấp 6-7 lần răng thật). Nhờ đó, khách hàng có thể ăn nhai thoải mái mọi món ăn dai cứng mà không lo gãy vỡ, nứt mẻ.",
                "• Bước 1: Khám tổng quát răng miệng, chụp phim X-quang kiểm tra chân răng, tủy và tổ chức quanh răng.\n"
                        +
                        "• Bước 2: Vệ sinh khoang miệng, gây tê cục bộ không đau và sửa soạn cùi răng nhẹ nhàng theo tỷ lệ chuẩn bảo tồn tối đa.\n"
                        +
                        "• Bước 3: Quét dấu hàm bằng máy quét 3D kỹ thuật số và lắp răng tạm thẩm mỹ ngay trong ngày để bảo vệ răng.\n"
                        +
                        "• Bước 4: Thiết kế răng trên phần mềm CAD 3D và tiện phôi sứ tự động bằng máy phay 5 trục CAM tại Labo nha khoa chuyên sâu.\n"
                        +
                        "• Bước 5: Thử răng sứ trên miệng, kiểm tra thẩm mỹ màu sắc, độ khít sát viền nướu và tinh chỉnh khớp cắn chuẩn.\n"
                        +
                        "• Bước 6: Gắn răng vĩnh viễn bằng xi-măng nha khoa sinh học cao cấp và kích hoạt thẻ bảo hành chính hãng điện tử.",
                "• Độ bền cơ học siêu việt: Chịu lực nhai gấp 6-7 lần răng thật, thoải mái thưởng thức đồ ăn nóng lạnh.\n"
                        +
                        "• Tính thẩm mỹ đỉnh cao: Rìa cắn trong bóng, vân răng đa tầng sinh động, không đục giả.\n" +
                        "• Không đen viền nướu theo thời gian, không gây hôi miệng khi được gắn khít sát tuyệt đối.\n" +
                        "• Tuổi thọ răng sứ kéo dài từ 15 đến 25 năm hoặc trọn đời nếu chăm sóc tốt.\n" +
                        "• Khắc phục hoàn hảo các khuyết điểm răng gãy mẻ, răng sâu vỡ lớn hoặc răng lệch lạc nhẹ.",
                "• Răng bị sâu vỡ mảng lớn không thể hàn trám composite.\n" +
                        "• Răng đã điều trị tủy giòn xốp, dễ gãy cần được bọc chụp sứ bảo vệ thân răng vĩnh viễn.\n" +
                        "• Răng ố vàng, nhiễm fluor hoặc tetracycline nặng.\n" +
                        "• Hàm răng khấp khểnh, chen chúc nhẹ muốn cải thiện thẩm mỹ nhanh chóng trong 3 ngày mà không cần niềng.",
                "• Cần tái khám ngay nếu cảm thấy cộm vướng khớp cắn khi nhai thức ăn.\n" +
                        "• Hạn chế ăn đồ quá cứng trong 24 giờ đầu sau khi vừa gắn răng sứ vĩnh viễn.",
                "• Vệ sinh răng miệng tối thiểu 2 lần mỗi ngày sau bữa ăn.\n" +
                        "• Kết hợp sử dụng chỉ nha khoa và máy tăm nước để làm sạch hoàn toàn thức ăn đọng ở kẽ răng.\n"
                        +
                        "• Khám định kỳ và lấy vôi răng 6 tháng một lần để bảo vệ sức khỏe nướu răng.");

        // 5. Tẩy Trắng Răng Laser Whitening
        upsertService(
                "Tẩy Trắng Răng Laser Whitening Hoa Kỳ Không Ê Buốt",
                1500000.0,
                "45 - 60 phút",
                "https://images.unsplash.com/photo-1588776814546-1ffcf47267a5?auto=format&fit=crop&w=800&q=80",
                "Bật sáng nụ cười rạng rỡ từ 3-5 tông màu chỉ sau 45 phút, an toàn tuyệt đối cho men răng nhờ công nghệ Laser ánh sáng lạnh và gel bù khoáng Fluoride.",
                "Tẩy trắng răng Laser Whitening tại C7 Smile Clinic áp dụng công nghệ quang hoạt hóa hiện đại từ Hoa Kỳ. Cơ chế hoạt động thông minh sử dụng bước sóng ánh sáng laser lạnh an toàn kết hợp với gel tẩy trắng chính hãng được kiểm định bởi Hiệp hội Nha khoa Hoa Kỳ (ADA) để bẻ gãy các chuỗi protein liên kết sắc tố ố vàng nằm sâu bên trong cấu trúc men răng.\n\n"
                        +
                        "Phương pháp này hoàn toàn không làm mòn men hay tổn thương tủy răng. Trước khi chiếu laser, nướu răng được cách ly và bảo vệ 100% bằng gel Dam chuyên dụng. Kết thúc liệu trình, răng được thoa lớp khoáng chất bù Fluoride giúp men răng chắc khỏe, bóng sáng và chấm dứt hoàn toàn cảm giác ê buốt khó chịu.",
                "• Bước 1: Khám tổng quát răng miệng, làm sạch vôi răng mảng bám và so màu răng ban đầu bằng bảng so màu Vita chuẩn y khoa.\n"
                        +
                        "• Bước 2: Đặt banh môi mềm mại và quét hợp chất Gel Dam cách ly nướu tuyệt đối an toàn.\n" +
                        "• Bước 3: Thoa đều lớp gel tẩy trắng răng nồng độ an toàn theo chỉ định lên toàn bộ mặt ngoài cung răng.\n"
                        +
                        "• Bước 4: Chiếu ánh sáng Laser lạnh kích hoạt các hoạt chất tẩy trắng trong 15-20 phút (thực hiện từ 2 đến 3 chu kỳ lặp lại).\n"
                        +
                        "• Bước 5: Làm sạch lớp gel, thoa kem bù khoáng chống ê buốt cấp tốc và dưỡng men răng sáng bóng.\n"
                        +
                        "• Bước 6: So màu răng sau khi tẩy trắng, chụp ảnh lưu hồ sơ và hướng dẫn chế độ kiêng màu sau điều trị.",
                "• Bật sáng tức thì từ 3 đến 5 tông màu chỉ sau duy nhất một liệu trình 45 phút.\n" +
                        "• Công nghệ Laser lạnh không sinh nhiệt, êm ái, hoàn toàn không đau buốt.\n" +
                        "• Tăng cường độ cứng của men răng nhờ dưỡng chất bù khoáng sinh học.\n" +
                        "• Hiệu quả duy trì độ trắng sáng tự nhiên bền lâu từ 2 đến 3 năm.\n" +
                        "• Tiết kiệm tối đa thời gian, phù hợp cho người chuẩn bị cưới hỏi, sự kiện quan trọng.",
                "• Răng bị ố vàng, xỉn màu do thói quen uống trà, cà phê, rượu vang hoặc hút thuốc lá.\n" +
                        "• Răng ngả vàng tự nhiên do tuổi tác hoặc vệ sinh răng miệng chưa đúng cách.\n" +
                        "• Khách hàng có nhu cầu làm mới nụ cười rạng rỡ cấp tốc cho công việc hoặc các dịp lễ tiệc.",
                "• Không áp dụng cho phụ nữ có thai hoặc đang cho con bú, người dưới 16 tuổi.\n" +
                        "• Khách hàng đang có ổ sâu răng lớn hoặc viêm nướu nặng cần được điều trị ổn định trước khi tẩy trắng.",
                "• Áp dụng 'chế độ ăn màu trắng' trong 48 giờ đầu: Ăn cơm, sữa tươi, thịt gà, cá, chuối, nước lọc...\n"
                        +
                        "• Tránh các thực phẩm và đồ uống sẫm màu như cà phê, trà đậm, nước tương, tương ớt, socola trong 3-5 ngày.\n"
                        +
                        "• Uống nước bằng ống hút để hạn chế tiếp xúc trực tiếp chất màu lên men răng mới tẩy.");

        // 6. Nhổ Răng Khôn Sóng Siêu Âm Piezotome
        upsertService(
                "Nhổ Răng Khôn Sóng Siêu Âm Piezotome Không Đau",
                1200000.0,
                "15 - 30 phút / răng",
                "https://images.unsplash.com/photo-1629909615184-74f495363b67?auto=format&fit=crop&w=800&q=80",
                "Xóa tan nỗi sợ nhổ răng khôn nhờ bước sóng siêu âm Piezotome thông minh, không rạch mổ lớn, không đau, giảm sưng nề 90%, lành thương thần tốc.",
                "Nhổ răng khôn bằng sóng siêu âm Piezotome là cuộc cách mạng trong phẫu thuật nha khoa. Thay vì dùng búa, kìm bẩy cơ học truyền thống gây sang chấn nặng nề, máy Piezotome sử dụng các đầu mũi rung siêu âm tần số chọn lọc 28 - 36 KHz tác động nhẹ nhàng làm đứt các dây chằng nha chu quanh chân răng.\n\n"
                        +
                        "Công nghệ này hoàn toàn không làm tổn thương các mô mềm, nướu, mạch máu và hệ thống dây thần kinh hàm dưới. Kết hợp với việc đặt màng huyết tương giàu tiểu cầu PRF tự thân vào ổ răng sau khi nhổ giúp cầm máu ngay lập tức, kích thích tái tạo mô hạt và xương nhanh gấp 3 lần, giúp bạn thoải mái đi làm và sinh hoạt bình thường ngay ngày hôm sau.",
                "• Bước 1: Khám tổng quát, đo huyết áp và chụp phim CT ConeBeam 3D khảo sát độ nghiêng chân răng và vị trí dây thần kinh.\n"
                        +
                        "• Bước 2: Vệ sinh sát khuẩn khoang miệng và thực hiện gây tê vi điểm không đau bằng công nghệ kim siêu mịn.\n"
                        +
                        "• Bước 3: Kích hoạt sóng siêu âm Piezotome nhẹ nhàng bóc tách dây chằng quanh răng và chia cắt thân răng nhanh gọn.\n"
                        +
                        "• Bước 4: Nhấc từng phần chân răng ra ngoài nhẹ nhàng mà không gây tổn thương xương ổ răng xung quanh.\n"
                        +
                        "• Bước 5: Bơm rửa sạch ổ răng, đặt màng sinh học PRF tự thân cầm máu và thúc đẩy liền thương cấp tốc.\n"
                        +
                        "• Bước 6: Khâu đóng kín vết mổ bằng chỉ tự tiêu vi phẫu thẩm mỹ, hướng dẫn chườm đá lạnh tại phòng khám.",
                "• Thời gian nhổ siêu nhanh: Chỉ từ 5 đến 15 phút cho một ca răng khôn thông thường.\n" +
                        "• Không đau đớn, không sang chấn cơ học, loại bỏ hoàn toàn nỗi ám ảnh tiếng búa gõ lạch cạch.\n"
                        +
                        "• Hạn chế tối đa sưng má, bầm tím, sau nhổ có thể nói chuyện và ăn uống bình thường.\n" +
                        "• Bảo vệ an toàn tuyệt đối các dây thần kinh cảm giác và mô nướu lân cận.\n" +
                        "• Liền thương cực nhanh nhờ công nghệ huyết tương giàu tiểu cầu PRF.",
                "• Răng khôn mọc lệch, mọc ngầm, đâm ngang 90 độ vào chân răng số 7 gây giắt thức ăn và sâu răng bên cạnh.\n"
                        +
                        "• Răng khôn bị viêm lợi trùm tái phát nhiều lần, gây sưng sốt, đau nhức khó há miệng.\n" +
                        "• Răng khôn bị sâu vỡ mảng lớn gây viêm tủy.\n" +
                        "• Chỉ định nhổ răng khôn để tạo khoảng dịch chuyển khi niềng răng chỉnh nha.",
                "• Khách hàng nên ăn nhẹ trước khi đến nhổ răng và giữ tâm lý thoải mái, tự tin.\n" +
                        "• Không nên nhổ răng khi cơ thể đang sốt cao hoặc phụ nữ đang trong chu kỳ kinh nguyệt.",
                "• Cắn chặt miếng gạc cầm máu tại chỗ trong 45 - 60 phút, nuốt nước bọt bình thường, không khạc nhổ.\n"
                        +
                        "• Không dùng ống hút, không súc miệng bằng nước muối tự pha trong 24 giờ đầu để bảo vệ cục máu đông.\n"
                        +
                        "• Chườm đá lạnh ngoài má trong ngày đầu tiên, các ngày sau chườm ấm để tan máu bầm nếu có.\n" +
                        "• Uống thuốc kháng viêm giảm đau theo đúng toa chỉ dẫn của bác sĩ điều trị.");

        // 7. Điều Trị Tủy Vi Phẫu Dental Dam
        upsertService(
                "Điều Trị Tủy Công Nghệ Vi Phẫu Dental Dam Triệt Để",
                800000.0,
                "45 - 60 phút / buổi (1 - 2 lần hẹn)",
                "https://images.unsplash.com/photo-1598256989800-fe5f95da9787?auto=format&fit=crop&w=800&q=80",
                "Dứt điểm cơn đau buốt răng dữ dội với kính hiển vi vi phẫu quang học, làm sạch 100% vi khuẩn ống tủy, bảo tồn tối đa răng thật nguyên bản.",
                "Viêm tủy răng là bệnh lý nha khoa nguy hiểm nếu không chữa trị kịp thời sẽ gây đau nhức buốt tận óc, hình thành nang chân răng, tiêu xương hàm và nguy cơ mất răng vĩnh viễn. Tại C7 Smile Clinic, dịch vụ điều trị tủy được nâng tầm với công nghệ vi phẫu chuẩn Châu Âu.\n\n"
                        +
                        "Quy trình được cách ly tuyệt đối bằng đê cao su (Dental Dam) ngăn chặn nước bọt và vi khuẩn xâm nhập vào buồng tủy. Máy định vị chóp điện tử đo chính xác chiều dài chân răng đến 0.1mm kết hợp hệ thống trâm quay WaveOne Gold siêu mềm dẻo giúp bác sĩ làm sạch triệt để các nhánh tủy phụ phức tạp nhất mà không làm thủng thành ống tủy. Bít kín hệ thống ống tủy 3D bằng Gutta-percha nóng chảy sinh học giúp dứt điểm cơn đau và ngăn ngừa tái phát vĩnh viễn.",
                "• Bước 1: Thăm khám lâm sàng, chụp phim X-quang xác định mức độ viêm nhiễm cuống răng và số lượng ống tủy.\n"
                        +
                        "• Bước 2: Gây tê cục bộ nhẹ nhàng triệt tiêu hoàn toàn cảm giác ê buốt và đặt đê cao su Dental Dam cách ly răng tuyệt đối.\n"
                        +
                        "• Bước 3: Mở đường vào buồng tủy, sử dụng máy định vị chóp điện tử đo chính xác chiều dài từng ống tủy.\n"
                        +
                        "• Bước 4: Tạo hình và làm sạch buồng tủy bằng trâm quay siêu dẻo WaveOne kết hợp dung dịch bơm rửa siêu âm kích hoạt khử trùng.\n"
                        +
                        "• Bước 5: Bít kín hệ thống ống tủy 3 chiều bằng vật liệu sinh học Gutta-percha nóng chảy không co ngót.\n"
                        +
                        "• Bước 6: Chụp phim kiểm tra chất lượng bít tủy và trám phục hồi thân răng bằng Composite thẩm mỹ hoặc chỉ định bọc mão sứ bảo vệ.",
                "• Cắt đứt ngay lập tức cơn đau nhức buốt răng sau buổi điều trị đầu tiên.\n" +
                        "• Bảo tồn tối đa răng thật nguyên vẹn, tránh nguy cơ phải nhổ bỏ răng sớm.\n" +
                        "• Thao tác chính xác 100% dưới kính hiển vi vi phẫu, tỷ lệ thành công đạt trên 99.4%.\n" +
                        "• Không gây biến chứng thủng sàn buồng tủy hay gãy dụng cụ trong ống tủy.\n" +
                        "• Quy trình vô trùng tuyệt đối, an toàn cho sức khỏe người bệnh.",
                "• Răng bị sâu ăn sâu vào tủy gây đau nhức từng cơn, đau dữ dội về đêm.\n" +
                        "• Răng bị chấn thương mẻ vỡ lớn làm lộ tủy răng ra ngoài.\n" +
                        "• Răng bị viêm tủy hoại tử không đau nhưng xuất hiện mụn mủ ở nướu chân răng.\n" +
                        "• Răng từng chữa tủy ở nơi khác nhưng bị đau nhức trở lại do sót ống tủy.",
                "• Răng sau khi điều trị tủy sẽ trở nên giòn và dễ nứt vỡ hơn răng sống, do đó nên bọc răng sứ để bảo vệ thân răng lâu dài.\n"
                        +
                        "• Tránh dùng răng đang chữa tủy nhai thức ăn cứng dai.",
                "• Uống thuốc kháng viêm theo đơn bác sĩ nếu cảm thấy hơi tức nhẹ chân răng trong 1-2 ngày đầu.\n" +
                        "• Vệ sinh răng miệng sạch sẽ bằng bàn chải mềm và súc miệng nước muối ấm.\n" +
                        "• Tái khám chụp phim kiểm tra định kỳ 6 tháng một lần.");

        // 8. Nha Khoa Trẻ Em & Tiền Chỉnh Nha Myobrace
        upsertService(
                "Nha Khoa Trẻ Em Thân Thiện & Tiền Chỉnh Nha Myobrace",
                500000.0,
                "30 - 45 phút",
                "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=800&q=80",
                "Trải nghiệm khám răng không đau - không nước mắt cho bé yêu, phòng ngừa sâu răng và nắn chỉnh xương hàm thẳng đều ngay từ nhỏ.",
                "Nha khoa trẻ em tại C7 Smile Clinic được thiết kế với không gian sắc màu vui tươi, thân thiện như một khu vui chơi mini. Đội ngũ bác sĩ nha khoa nhi với kỹ năng tâm lý sư phạm xuất sắc sẽ biến mỗi lần khám răng thành một buổi trải nghiệm khám phá thú vị, giúp bé hoàn toàn thoải mái và xóa bỏ mọi nỗi sợ hãi với áo blouse trắng.\n\n"
                        +
                        "Bên cạnh các dịch vụ dự phòng sâu răng như bôi Vecni Fluoride và trám bít hố rãnh, C7 Smile đặc biệt chú trọng can thiệp tiền chỉnh nha sớm bằng khí cụ Myobrace cho trẻ từ 5-12 tuổi. Phương pháp này giúp điều chỉnh triệt để các thói quen xấu của trẻ (như thở miệng, mút ngón tay, đẩy lưỡi, nuốt sai cách), kích thích xương hàm phát triển cân đối và định hướng răng vĩnh viễn mọc thẳng đều tự nhiên mà không cần phải nhổ răng khi trưởng thành.",
                "• Bước 1: Bác sĩ làm quen, trò chuyện và cùng bé tham gia trò chơi tương tác vui vẻ để tạo sự tin tưởng.\n"
                        +
                        "• Bước 2: Khám răng nhẹ nhàng, kiểm tra số lượng răng sữa, phát hiện sớm mầm mống sâu răng và sai lệch khớp cắn.\n"
                        +
                        "• Bước 3: Thoa gel gây tê có mùi thơm trái cây ngọt ngào trước khi thực hiện các thủ thuật vệ sinh hoặc trám răng.\n"
                        +
                        "• Bước 4: Hàn trám răng phòng ngừa bằng vật liệu thân thiện sinh học hoặc quét mẫu hàm kỹ thuật số lựa chọn khí cụ Myobrace phù hợp.\n"
                        +
                        "• Bước 5: Hướng dẫn bé bài tập thở qua mũi, tập nuốt đúng và kỹ năng chải răng xoay tròn đúng chuẩn.\n"
                        +
                        "• Bước 6: Tặng quà bé dũng cảm, chụp ảnh kỷ niệm và hẹn lịch tái khám định kỳ mỗi 3 - 6 tháng.",
                "• Tạo lập tâm lý vui vẻ, bé yêu thích việc chăm sóc răng miệng và không bao giờ sợ hãi nha sĩ.\n" +
                        "• Bảo vệ hàm răng sữa chắc khỏe, đảm bảo chức năng ăn nhai tiêu hóa tốt cho trẻ phát triển thể chất.\n"
                        +
                        "• Tiền chỉnh nha can thiệp sớm giúp gương mặt phát triển cân đối, tiết kiệm đến 70% chi phí niềng răng sau này.\n"
                        +
                        "• Bổ sung men Fluoride giúp răng bé chống lại sự tấn công của vi khuẩn sâu răng lên đến 80%.\n"
                        +
                        "• Sử dụng vật liệu nha khoa sinh học cao cấp, an toàn tuyệt đối cho trẻ nhỏ.",
                "• Trẻ em từ 3 đến 14 tuổi cần kiểm tra răng định kỳ.\n" +
                        "• Trẻ có thói quen xấu: Thở bằng miệng khi ngủ, mút ngón tay, cắn môi, đẩy lưỡi.\n" +
                        "• Trẻ có dấu hiệu răng mọc lệch lạc, hô hàm, răng thưa hoặc khớp cắn ngược.\n" +
                        "• Trẻ bị sâu răng sữa, sún răng, đau răng cần điều trị nhẹ nhàng.",
                "• Ba mẹ tuyệt đối không dùng các từ dọa nạt như 'đi tiêm kim', 'bị nhổ răng' trước khi đưa bé đến phòng khám.\n"
                        +
                        "• Chuẩn bị tâm lý cho bé như đi tham gia một buổi khám phá nụ cười bổ ích.",
                "• Nhắc nhở và đồng hành cùng bé đánh răng 2 lần mỗi ngày sau bữa ăn sáng và trước khi đi ngủ.\n" +
                        "• Hạn chế cho bé ăn bánh kẹo ngọt, nước ngọt có ga vào buổi tối.\n" +
                        "• Khám răng định kỳ 3 - 6 tháng một lần để phát hiện và xử lý sớm các vấn đề về răng miệng.");

        System.out.println("✅ Successfully seeded 8 rich dental services!");
    }

    private void upsertService(String name, Double price, String duration, String imageUrl,
            String shortDesc, String detailedDesc, String process,
            String benefits, String suitableFor, String notes, String aftercare) {
        Service svc = serviceRepository.findAll().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(new Service());

        svc.setName(name);
        svc.setPrice(price);
        svc.setDuration(duration);
        svc.setImageUrl(imageUrl);
        svc.setShortDescription(shortDesc);
        svc.setDescription(shortDesc);
        svc.setDetailedDescription(detailedDesc);
        svc.setProcess(process);
        svc.setBenefits(benefits);
        svc.setSuitableFor(suitableFor);
        svc.setNotes(notes);
        svc.setAftercare(aftercare);
        svc.setStatus("Hoạt động");

        serviceRepository.save(svc);
    }
}
