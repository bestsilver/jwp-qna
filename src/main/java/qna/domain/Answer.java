package qna.domain;

import qna.CannotDeleteException;
import qna.NotFoundException;
import qna.UnAuthorizedException;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
@Entity
@Table(name="answer")
public class Answer extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "writer_id", foreignKey = @ForeignKey(name = "fk_answer_writer"))
    private User writer;
    @ManyToOne
    @JoinColumn(name = "question_id", foreignKey = @ForeignKey(name = "fk_answer_to_question"))
    private Question question;
    @Lob
    private String contents;
    @Column(nullable = false)
    private boolean deleted = false;

    private static final String DELETE_ERROR = "다른 사람이 쓴 답변이 있어 삭제할 수 없습니다.";

    protected Answer() {
    }

    public Answer(User writer, Question question, String contents) {
        this(null, writer, question, contents);
    }

    public Answer(Long id, User writer, Question question, String contents) {
        this.id = id;

        if (Objects.isNull(writer)) {
            throw new UnAuthorizedException();
        }

        if (Objects.isNull(question)) {
            throw new NotFoundException();
        }

        this.writer = writer;
        this.question = question;
        this.contents = contents;
    }

    public boolean isOwner(User writer) {
        return this.writer.equals(writer);
    }

    public void toQuestion(Question question) {
        this.question = question;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getWriter() {
        return writer;
    }

    public void setWriter(User writer) {
        this.writer = writer;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public DeleteHistory delete(User deleter) throws CannotDeleteException {
        if (!deleter.equals(writer)) {
            throw new CannotDeleteException(DELETE_ERROR);
        }

        this.deleted = true;
        return new DeleteHistory(ContentType.ANSWER, this.getId(), this.getWriter(), LocalDateTime.now());
    }

    @Override
    public String toString() {
        return "Answer{" +
                "id=" + id +
                ", writerId=" + writer +
                ", questionId=" + question +
                ", contents='" + contents + '\'' +
                ", deleted=" + deleted +
                '}';
    }
}
