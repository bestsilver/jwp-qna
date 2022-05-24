package qna.repository;

import org.hibernate.PropertyValueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import qna.CannotDeleteException;
import qna.domain.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class QuestionRepositoryTest {
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnswerRepository answerRepository;
    private User writer;

    @DisplayName("테스트 메세드 실행 전 변수 초기화")
    @BeforeEach
    void setup() {
        writer = userRepository.save(new User(3L, "bestsilver", "password", "name", "bestsilver@ggg.net"));
    }

    @Test
    void 질문_등록_테스트() {
        Question question = new Question("title1", "contents1").writeBy(writer);
        assertThat(question.isOwner(writer)).isTrue();
    }

    @DisplayName("not null인 필드에 null을 넣었을 때")
    @Test
    void 질문_title_null_등록_테스트() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            Question question = new Question(null, "content3").writeBy(writer);
            questionRepository.save(question);
        });
    }

    @DisplayName("deleted가 false인 객체들 검색")
    @Test
    void findByDeletedFalse_테스트() {
        Question question1 = new Question("title1", "content1").writeBy(writer);
        Question question2 = new Question("title3", "content3").writeBy(writer);

        questionRepository.save(question1);
        questionRepository.save(question2);

        List<Question> byDeletedFalse = questionRepository.findByDeletedFalse();

        assertThat(byDeletedFalse.size()).isEqualTo(2);
    }

    @Test
    void findByIdAndDeletedFalse_테스트() {
        Question question = new Question("title1", "contents1").writeBy(writer);
        questionRepository.save(question);
        Optional<Question> byIdAndDeletedFalse = questionRepository.findByIdAndDeletedFalse(question.getId());

        assertThat(byIdAndDeletedFalse.isPresent()).isTrue();
        assertThat(byIdAndDeletedFalse.get() == question).isTrue();
    }

    @Test
    void title_Over_Max_Length_테스트() {

        String title = new Random().ints(97, 122 + 1)
                .limit(200)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        assertThrows(DataIntegrityViolationException.class, () -> {
            Question question = new Question(title,"content1").writeBy(writer);
            questionRepository.save(question);
        });
    }

    @Test
    void 작성자가_아닐_경우_Question_삭제_에러_throw() {
        User user = new User(3L, "gone", "password", "name", "gone@ggg.net");

        Question question = new Question("title1", "contents1").writeBy(writer);
        assertThrows(CannotDeleteException.class, () -> {
            question.delete(user);
        });
    }

    @Transactional
    @Test
    void Question에_타인이_작성한_Answer가_달릴_경우_삭제_에러_throw() {
        Question question = new Question("title1", "contents1").writeBy(writer);
        Answer answer = new Answer(UserTest.JAVAJIGI, question, "Answers Contents2");
        question.addAnswer(answer);

        assertThrows(CannotDeleteException.class, () -> {
            question.delete(writer);
        });
    }

    @Test
    void Question에_본인이_작성한_Answer만_있을_경우_삭제_가능() throws CannotDeleteException {
        Question question = new Question("title1", "contents1").writeBy(writer);
        Answer answer = new Answer(writer, question, "Answers Contents2");
        question.addAnswer(answer);
        question.delete(writer);

        assertThat(question.isDeleted()).isTrue();
    }

    @Test
    void Question이_삭제될_때_Answer도_함께_삭제_체크() throws CannotDeleteException {
        Question question = questionRepository.save(new Question("title1", "contents1").writeBy(writer));
        Answer answer = answerRepository.save(new Answer(writer, question, "Answers Contents2"));
        question.addAnswer(answer);
        question.delete(writer);
        List<Answer> byQuestionIdAndDeletedFalse = answerRepository.findByQuestionIdAndDeletedFalse(question.getId());

        assertThat(byQuestionIdAndDeletedFalse.size()).isEqualTo(0);

    }
}