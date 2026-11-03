package com.swp391.kpi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * Them 1 anh hoac 1 video vao bai (UC-03, UC-04 buoc 4). Gui dang multipart/form-data.
 * Anh: gui file (JPG/PNG, toi da 10 MB - A1). Video: chi nhap link (A3). Gui dung 1 trong 2, kiem trong service.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class ArticleAttachmentUploadRequest {

    private MultipartFile file;          // anh JPG/PNG

    @Size(max = 500, message = "Link video tối đa 500 ký tự")
    private String videoUrl;             // link video (A3)

    @NotBlank(message = "Vui lòng chọn loại ảnh/video")
    private String mediaKind;            // loai tac pham, tra bang media_factors (PHOTO, GRAPHIC, VIDEO...)

    @NotNull(message = "Vui lòng chọn tác giả của ảnh/video")
    private Integer authorId;            // nguoi nhan nhuan but phan nay, phai la tac gia cua bai (BR-IN-05)

    @Size(max = 200, message = "Nguồn ngoài tối đa 200 ký tự")
    private String externalSource;       // co = lay tu nguon ngoai, khong tinh tien (BR-IN-05)

    @Size(max = 300, message = "Chú thích tối đa 300 ký tự")
    private String caption;
}
