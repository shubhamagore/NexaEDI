import { useState, useRef, type DragEvent, type ChangeEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Upload, FileText, Zap, CheckCircle2, Copy, ArrowRight, X, AlertCircle } from 'lucide-react';
import { ingestEdi } from '../api/client';
import type { ProcessingResponse } from '../types';

const SAMPLE_EDI = `ISA*00*          *00*          *ZZ*TARGET         *ZZ*VENDORABC      *260219*1200*^*00501*000000042*0*P*>~GS*PO*TGTBUY*VENDORABC*20260219*1200*42*X*005010~ST*850*0001~BEG*00*SA*TGT-2026-00042**20260219~REF*DP*042~DTM*002*20260305~N1*ST*Target Store #1742*92*1742~N3*700 Nicollet Mall~N4*Minneapolis*MN*55402~PO1*1*120*EA*24.99**UI*089541234567~PO1*2*60*EA*49.99**UI*089599876543~CTT*2~SE*11*0001~GE*1*42~IEA*1*000000042~`;

const RETAILERS = ['TARGET', 'WALMART', 'AMAZON', 'COSTCO'];

export default function Ingest() {
  const navigate = useNavigate();
  const fileRef = useRef<HTMLInputElement>(null);

  const [retailerId, setRetailerId] = useState('TARGET');
  const [customRetailer, setCustomRetailer] = useState('');
  const [ediContent, setEdiContent] = useState('');
  const [fileName, setFileName] = useState('upload.edi');
  const [isDragging, setIsDragging] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<ProcessingResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const effectiveRetailer = customRetailer.trim() || retailerId;

  const handleFile = (file: File) => {
    setFileName(file.name);
    const reader = new FileReader();
    reader.onload = e => setEdiContent(e.target?.result as string ?? '');
    reader.readAsText(file);
  };

  const onDrop = (e: DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  };

  const onFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
  };

  const handleSubmit = async () => {
    if (!ediContent.trim()) { toast.error('EDI content is required'); return; }
    if (!effectiveRetailer) { toast.error('Retailer ID is required'); return; }

    setSubmitting(true);
    setError(null);
    setResult(null);

    try {
      const res = await ingestEdi({ retailerId: effectiveRetailer, ediContent, fileName });
      setResult(res);
      toast.success('EDI file accepted for processing!');
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? err.message ?? 'Submission failed';
      setError(msg);
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    toast.success('Copied to clipboard');
  };

  const reset = () => { setResult(null); setError(null); setEdiContent(''); setFileName('upload.edi'); };

  return (
    <div className="max-w-4xl space-y-6 animate-slide-in">
      {/* Success Result */}
      {result && (
        <div className="card p-6 border-emerald-200 bg-emerald-50">
          <div className="flex items-start gap-4">
            <CheckCircle2 className="w-6 h-6 text-emerald-600 shrink-0 mt-0.5" />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold text-emerald-900">EDI File Accepted</p>
              <p className="text-xs text-emerald-700 mt-1">{result.message}</p>

              <div className="mt-4 bg-white rounded-lg border border-emerald-200 p-4 space-y-3">
                <div>
                  <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">Correlation ID</p>
                  <div className="flex items-center gap-2">
                    <code className="text-sm font-mono text-slate-900 break-all">{result.correlationId}</code>
                    <button onClick={() => copyToClipboard(result.correlationId)} className="text-slate-400 hover:text-slate-600 shrink-0">
                      <Copy className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
                <div>
                  <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">Accepted At</p>
                  <p className="text-sm text-slate-700">{new Date(result.acceptedAt).toLocaleString()}</p>
                </div>
              </div>

              <div className="flex gap-3 mt-4">
                <button onClick={() => navigate(`/audit/${result.correlationId}`)} className="btn-primary">
                  View Audit Trail <ArrowRight className="w-4 h-4" />
                </button>
                <button onClick={reset} className="btn-secondary">
                  Submit Another
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="card p-4 border-red-200 bg-red-50 flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-red-500 shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-red-900">Submission Failed</p>
            <p className="text-xs text-red-700 mt-1 font-mono">{error}</p>
          </div>
          <button onClick={() => setError(null)}><X className="w-4 h-4 text-red-400" /></button>
        </div>
      )}

      {!result && (
        <>
          {/* Retailer Selection */}
          <div className="card p-5">
            <h2 className="text-sm font-semibold text-slate-900 mb-4">1. Select Retailer</h2>
            <div className="flex flex-wrap gap-2 mb-3">
              {RETAILERS.map(r => (
                <button
                  key={r}
                  onClick={() => { setRetailerId(r); setCustomRetailer(''); }}
                  className={`px-4 py-2 rounded-lg text-sm font-semibold border transition-all ${retailerId === r && !customRetailer ? 'bg-indigo-600 text-white border-indigo-600 shadow-sm' : 'bg-white text-slate-600 border-slate-200 hover:border-indigo-300 hover:text-indigo-600'}`}
                >
                  {r}
                </button>
              ))}
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-400">or custom:</span>
              <input
                className="input max-w-xs"
                placeholder="e.g., NORDSTROM"
                value={customRetailer}
                onChange={e => setCustomRetailer(e.target.value.toUpperCase())}
              />
            </div>
            <p className="text-xs text-slate-400 mt-2">
              Active: <span className="font-semibold text-slate-700">{effectiveRetailer}</span>
            </p>
          </div>

          {/* EDI Content */}
          <div className="card p-5">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-slate-900">2. EDI Content</h2>
              <div className="flex gap-2">
                <button
                  onClick={() => { setEdiContent(SAMPLE_EDI); setFileName('target-850-sample.edi'); setRetailerId('TARGET'); setCustomRetailer(''); }}
                  className="btn-secondary text-xs py-1.5"
                >
                  <Zap className="w-3.5 h-3.5" /> Use Sample (Target 850)
                </button>
                <button onClick={() => fileRef.current?.click()} className="btn-secondary text-xs py-1.5">
                  <Upload className="w-3.5 h-3.5" /> Upload File
                </button>
                <input ref={fileRef} type="file" accept=".edi,.txt,.x12" onChange={onFileChange} className="hidden" />
              </div>
            </div>

            {/* Drop Zone */}
            <div
              onDragOver={e => { e.preventDefault(); setIsDragging(true); }}
              onDragLeave={() => setIsDragging(false)}
              onDrop={onDrop}
              onClick={() => !ediContent && fileRef.current?.click()}
              className={`relative border-2 border-dashed rounded-xl transition-all ${isDragging ? 'border-indigo-400 bg-indigo-50' : ediContent ? 'border-slate-200 bg-white' : 'border-slate-200 bg-slate-50 hover:border-indigo-300 hover:bg-indigo-50/30 cursor-pointer'}`}
            >
              {ediContent ? (
                <div className="relative">
                  <textarea
                    value={ediContent}
                    onChange={e => setEdiContent(e.target.value)}
                    onClick={e => e.stopPropagation()}
                    className="w-full h-52 p-4 font-mono text-xs text-slate-800 bg-transparent resize-none focus:outline-none"
                    spellCheck={false}
                  />
                  <button
                    onClick={e => { e.stopPropagation(); setEdiContent(''); }}
                    className="absolute top-2 right-2 text-slate-400 hover:text-slate-600 bg-white rounded-md p-1 shadow-sm border border-slate-200"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              ) : (
                <div className="py-14 flex flex-col items-center gap-2">
                  <FileText className="w-8 h-8 text-slate-300" />
                  <p className="text-sm text-slate-500 font-medium">Drop your .edi file here, or click to browse</p>
                  <p className="text-xs text-slate-400">Supports X12 850, 856, 810 and all standard transaction sets</p>
                </div>
              )}
            </div>

            {/* Filename */}
            {ediContent && (
              <div className="mt-3 flex items-center gap-3">
                <label className="label mb-0">File Name</label>
                <input
                  className="input max-w-xs"
                  value={fileName}
                  onChange={e => setFileName(e.target.value)}
                  placeholder="filename.edi"
                />
                <p className="text-xs text-slate-400">{ediContent.length.toLocaleString()} characters</p>
              </div>
            )}
          </div>

          {/* Submit */}
          <div className="flex items-center gap-4">
            <button
              onClick={handleSubmit}
              disabled={submitting || !ediContent.trim()}
              className="btn-primary px-8 py-2.5"
            >
              {submitting ? (
                <><span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Processing...</>
              ) : (
                <><Zap className="w-4 h-4" /> Submit EDI File</>
              )}
            </button>
            <p className="text-xs text-slate-400">
              Processing is asynchronous — you'll receive a correlation ID to track progress.
            </p>
          </div>
        </>
      )}
    </div>
  );
}
