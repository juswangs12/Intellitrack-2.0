import React, { useState, useEffect } from 'react';
import api from '../services/ApiService';

const CommentSection = ({ submissionId }) => {
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (submissionId) {
      loadComments();
    }
  }, [submissionId]);

  const loadComments = async () => {
    try {
      setIsLoading(true);
      const data = await api.getCommentsForSubmission(submissionId);
      setComments(data || []);
    } catch (error) {
      console.error('Failed to load comments:', error);
      setComments([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmitComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim() || !submissionId) return;

    try {
      setIsSubmitting(true);
      await api.addCommentToSubmission(submissionId, { content: newComment.trim() });
      setNewComment('');
      await loadComments();
    } catch (error) {
      console.error('Failed to add comment:', error);
      alert('Failed to add comment. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (!submissionId) {
    return <div className="p-4 text-center text-gray-500">No submission selected</div>;
  }

  return (
    <div className="card" style={{ marginTop: '1.5rem' }}>
      <div className="card-header">
        <h3 className="card-title">Comments</h3>
      </div>
      <div className="card-body" style={{ padding: '1rem' }}>
        {/* Comment Form */}
        <form onSubmit={handleSubmitComment} className="mb-4">
          <div className="form-group">
            <textarea
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              placeholder="Add a comment..."
              className="form-control"
              disabled={isSubmitting}
              style={{ minHeight: '100px' }}
            />
          </div>
          <div className="text-end">
            <button
              type="submit"
              disabled={isSubmitting || !newComment.trim()}
              className="btn btn-primary"
            >
              {isSubmitting ? 'Sending...' : 'Post Comment'}
            </button>
          </div>
        </form>

        {/* Comments List */}
        <div>
          {isLoading ? (
            <div className="text-center py-4" style={{ color: '#6b7280' }}>Loading comments...</div>
          ) : comments.length === 0 ? (
            <div className="text-center py-4" style={{ color: '#6b7280' }}>No comments yet</div>
          ) : (
            comments.map((comment) => (
              <div key={comment.id} className="mb-3" style={{ border: '1px solid #e5e7eb', borderRadius: '0.75rem', padding: '1rem', background: '#f9fafb' }}>
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <div style={{ fontWeight: 600 }}>
                    {comment.authorFirstName} {comment.authorLastName}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                    {formatDate(comment.createdAt)}
                  </div>
                </div>
                <div style={{ color: '#111827', whiteSpace: 'pre-wrap' }}>
                  {comment.content}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default CommentSection;
