import type { DocumentFile } from "../../types";

type Props = {
  documents: DocumentFile[];
  selectedFileId: string | null;
  uploading: boolean;
  fileInputRef: React.RefObject<HTMLInputElement | null>;
  onUploadClick: () => void;
  onFileChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onSelect: (fileId: string) => void;
  onDelete: (documentFile: DocumentFile) => void;
};

export default function DocumentPanel({ documents, selectedFileId, uploading, fileInputRef, onUploadClick, onFileChange, onSelect, onDelete }: Props) {
  return (
    <aside className="documents-panel">
      <div className="documents-header">
        <div><h2>Documents</h2><span>{documents.length} in this conversation</span>{selectedFileId && <small className="selected-document-label">Document selected</small>}</div>
        <button className="upload-small" onClick={onUploadClick} disabled={uploading}>+</button>
      </div>

      <input ref={fileInputRef} type="file" accept="application/pdf" hidden onChange={onFileChange} />

      {uploading && <div className="uploading-card"><div className="mini-spinner"></div><div><strong>Indexing document</strong><span>Extracting and embedding PDF...</span></div></div>}

      {documents.length === 0 && !uploading && <div className="no-documents"><div className="document-empty-icon">◈</div><strong>No documents</strong><p>Upload a PDF to give this conversation a knowledge base.</p><button onClick={onUploadClick}>Upload PDF</button></div>}

      <div className="document-list">
        {documents.map((document) => (
          <div className={selectedFileId === document.fileId ? "document-card selected" : "document-card"} key={document.fileId} onClick={() => onSelect(document.fileId)}>
            <div className="pdf-icon">PDF</div>
            <div className="document-info"><strong title={document.fileName}>{document.fileName}</strong>{selectedFileId === document.fileId && <span className="selected-badge">Selected</span>}<span>{document.chunks} chunk{document.chunks !== 1 ? "s" : ""} · Indexed</span></div>
            <button className="delete-button" onClick={(event) => { event.stopPropagation(); onDelete(document); }} title="Delete document">🗑</button>
          </div>
        ))}
      </div>

      <div className="documents-footer">Documents are scoped to this conversation.</div>
    </aside>
  );
}
