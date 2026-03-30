package com.letschat.mvp_1.Service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.UploadUrlResponse;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public class UploadMediaService {

    private final S3Presigner presigner;

    @Value("${r2.bucket}")
    private String bucket;

    public UploadMediaService(S3Presigner presigner) {
        this.presigner = presigner;
    }

    public static String resolve(String mime) {

        if (mime.startsWith("image/"))
            return "images";

        if (mime.startsWith("video/"))
            return "videos";

        if (mime.startsWith("audio/"))
            return "audio";

        return "docs";
    }

    public UploadUrlResponse generateUploadUrls(String mime) {
        System.out.println(mime);
        String folder = resolve(mime);
        String ext = extensionFromMime(mime);
        String uuid = UUID.randomUUID().toString();

        String mainKey = folder + "/main/" + uuid + ext;
        String mainUrl = createPutUrl(mainKey, mime);

        String thumbKey = null;
        String thumbUrl = null;

        if (folder.equals("images") || folder.equals("videos")) {

            String thumbExt = ext;
            String thumbMime = mime;

            if (folder.equals("videos")) {
                thumbExt = ".png";
                thumbMime = "image/png";
            }

            thumbKey = folder + "/thumb/" + uuid + thumbExt;
            thumbUrl = createPutUrl(thumbKey, thumbMime);
        }

        return new UploadUrlResponse(
                mainKey,
                thumbKey,
                mainUrl,
                thumbUrl
        );
    }

    private String createPutUrl(String key, String mime) {

        PutObjectRequest objectRequest =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(mime)
                        .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(objectRequest)
                        .build();

        PresignedPutObjectRequest presignedRequest =
                presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toString();
    }

    public static String extensionFromMime(String mime) {

        switch (mime) {

            case "image/jpeg":
                return ".jpg";

            case "image/png":
                return ".png";

            case "image/webp":
                return ".webp";

            case "video/mp4":
                return ".mp4";

            case "audio/mpeg":
                return ".mp3";

            case "audio/ogg":
                return ".ogg";

            case "audio/wav":
                return ".wav";

            case "application/pdf":
                return ".pdf";

            default:
                return "";
        }
    }
}