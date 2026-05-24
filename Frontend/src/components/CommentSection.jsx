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
    <div className="bg-white rounded-lg shadow p-4">
      <h3 className="text-lg font-semibold mb-4">Comments</h3>

      {/* Comment Form */}
      <form onSubmit={handleSubmitComment} className="mb-4">
        <textarea
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="Add a comment..."
          className="w-full p-3 border rounded-lg mb-2 resize-vertical min-h-[80px]"
          disabled={isSubmitting}
        />
        <div className="flex justify-end">
          <button
            type="submit"
            disabled={isSubmitting || !newComment.trim()}
            className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Sending...' : 'Post Comment'}
          </button>
        </div>
      </form>

      {/* Comments List */}
      <div className="space-y-4">
        {isLoading ? (
          <div className="text-center text-gray-500 py-4">Loading comments...</div>
        ) : comments.length === 0 ? (
          <div className="text-center text-gray-500 py-4">No comments yet</div>
        ) : (
          comments.map((comment) => (
            <div key={comment.id} className="border rounded-lg p-3 bg-gray-50">
              <div className="flex items-center justify-between mb-2">
                <div className="font-medium">
                  {comment.authorFirstName} {comment.authorLastName}
                </div>
                <div className="text-xs text-gray-500">
                  {formatDate(comment.createdAt)}
                </div>
              </div>
              <div className="text-gray-700 whitespace-pre-wrap">
                {comment.content}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default CommentSection;
