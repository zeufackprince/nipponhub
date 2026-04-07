package com.nipponhub.nipponhubv0.Services;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.nipponhub.nipponhubv0.Models.FileDocument;
import com.nipponhub.nipponhubv0.Repositories.mongodb.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations gridFsOperations;
    private final FileRepository fileRepository;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".gif"};
    private static final byte[][] MAGIC_BYTES = {
        {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},  // JPEG
        {(byte) 0x89, 0x50, 0x4E, 0x47},          // PNG
        {0x47, 0x49, 0x46},                       // GIF
        {0x52, 0x49, 0x46, 0x46}                  // WEBP
    };

    // ─── UPLOAD ─────────────────────────────────────────────────────────────────

    /**
     * Upload a single image to MongoDB GridFS and save its metadata.
     *
     * @param file the multipart image file
     * @return the GridFS ObjectId as a String
     * @throws IOException              if reading the file stream fails
     * @throws IllegalArgumentException if the file is not an image
     */
    public String uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, null);
    }

    /**
     * Upload a single image linked to a specific product.
     *
     * @param file      the multipart image file
     * @param productId optional MySQL product id to associate with this file
     * @return the GridFS ObjectId as a String
     */
    

public String uploadFile(MultipartFile file, Long productId) throws IOException {
    // ─── VALIDATE FILE SIZE ──────────────────────────────────────────────
    if (file.getSize() > MAX_FILE_SIZE) {
        throw new IllegalArgumentException(
            "File size (" + file.getSize() + " bytes) exceeds limit of " + MAX_FILE_SIZE
        );
    }
    
    // ─── VALIDATE FILE EXTENSION ─────────────────────────────────────────
    String filename = file.getOriginalFilename().toLowerCase();
    boolean validExtension = false;
    for (String ext : ALLOWED_EXTENSIONS) {
        if (filename.endsWith(ext)) {
            validExtension = true;
            break;
        }
    }
    if (!validExtension) {
        throw new IllegalArgumentException(
            "File type not allowed. Accepted: " + String.join(", ", ALLOWED_EXTENSIONS)
        );
    }
    
    // ─── VALIDATE MAGIC BYTES ────────────────────────────────────────────
    byte[] fileHeader = new byte[4];
    try (InputStream is = file.getInputStream()) {
        is.read(fileHeader);
    }
    
    boolean validMagic = false;
    for (byte[] magic : MAGIC_BYTES) {
        if (startsWith(fileHeader, magic)) {
            validMagic = true;
            break;
        }
    }
    if (!validMagic) {
        throw new IllegalArgumentException("File is corrupted or not a valid image");
    }
    
    // ─── SANITIZE FILENAME ───────────────────────────────────────────────
    String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    String uniqueName = System.currentTimeMillis() + "_" + sanitized;
    
    // ─── STORE FILE ──────────────────────────────────────────────────────
    DBObject metadata = new BasicDBObject();
    metadata.put("originalName", sanitized);
    metadata.put("contentType", file.getContentType());
    metadata.put("size", file.getSize());
    if (productId != null) {
        metadata.put("productId", productId);
    }
    
    ObjectId fileId = gridFsTemplate.store(
        file.getInputStream(),
        uniqueName,
        file.getContentType(),
        metadata
    );
    
    return fileId.toHexString();
}

private boolean startsWith(byte[] data, byte[] prefix) {
    if (data.length < prefix.length) return false;
    for (int i = 0; i < prefix.length; i++) {
        if (data[i] != prefix[i]) return false;
    }
    return true;
}

    /**
     * Upload multiple images at once (no product association).
     */
    public List<String> uploadFiles(List<MultipartFile> files) throws IOException {
        return uploadFiles(files, null);
    }

    /**
     * Upload multiple images, all linked to the same product.
     */
    public List<String> uploadFiles(List<MultipartFile> files, Long productId) throws IOException {
        List<String> fileIds = new ArrayList<>();
        if (files == null || files.isEmpty()) return fileIds;

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                fileIds.add(uploadFile(file, productId));
            }
        }
        return fileIds;
    }

    // ─── RETRIEVE ────────────────────────────────────────────────────────────────

    /**
     * Retrieve a file's InputStream from GridFS by its ObjectId string.
     *
     * @throws IOException if the file is not found or stream can't be opened
     */
    public InputStream getFileById(String fileId) throws IOException {
        GridFSFile gridFSFile = gridFsTemplate.findOne(
            new Query(Criteria.where("_id").is(new ObjectId(fileId))) // ✅ ObjectId — not raw String
        );

        if (gridFSFile == null) {
            throw new IOException("File not found in GridFS: " + fileId);
        }

        return gridFsOperations.getResource(gridFSFile).getInputStream();
    }

    /**
     * Get the MIME content type of a stored file.
     * Falls back to "image/jpeg" if metadata is missing.
     *
     * FIX: was using raw String for _id — now correctly uses ObjectId.
     */
    public String getContentType(String fileId) {

        // ── Try metadata repo first (faster) ─────────────────────────────────
        return fileRepository.findByGridFsId(fileId)
            .map(FileDocument::getContentType)
            .orElseGet(() -> {
                // ── Fallback: query GridFS directly ──────────────────────────
                GridFSFile gridFSFile = gridFsTemplate.findOne(
                    new Query(Criteria.where("_id").is(new ObjectId(fileId))) // ✅ Fixed
                );

                if (gridFSFile == null || gridFSFile.getMetadata() == null) {
                    log.warn("Content type not found for fileId: {} — defaulting to image/jpeg", fileId);
                    return "image/jpeg";
                }

                return gridFSFile.getMetadata().getString("contentType");
            });
    }

    /**
     * Get all file metadata documents associated with a product.
     */
    public List<FileDocument> getFilesByProductId(Long productId) {
        return fileRepository.findAllByProductId(productId);
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────────

    /**
     * Delete a single file from GridFS and remove its metadata document.
     *
     * FIX: was using raw String for _id — now correctly uses ObjectId.
     */
    public void deleteFile(String fileId) {
        // ── Delete from GridFS ────────────────────────────────────────────────
        gridFsTemplate.delete(
            new Query(Criteria.where("_id").is(new ObjectId(fileId))) // ✅ Fixed
        );

        // ── Delete metadata document ──────────────────────────────────────────
        fileRepository.deleteByGridFsId(fileId);

        log.info("File deleted — gridFsId: {}", fileId);
    }

    /**
     * Delete multiple files at once.
     */
    public void deleteFiles(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return;
        for (String fileId : fileIds) {
            deleteFile(fileId);
        }
    }
}
