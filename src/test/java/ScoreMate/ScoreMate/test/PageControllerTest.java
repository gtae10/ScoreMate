package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.web.PageController;
import ScoreMate.ScoreMate.web.PlayerPageController;
import ScoreMate.ScoreMate.web.StandingsPageController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 대시보드/순위표/선수 검색 페이지의 컨트롤러가 올바른 뷰 이름과 모델 속성을
 * 반환하는지 확인한다. MockMvc(HTTP 계층) 대신 컨트롤러 메서드를 직접 호출하는
 * 방식이라 별도 웹 테스트 모듈 의존성 없이 가볍게 돈다.
 *
 * 외부 네트워크 호출이 없는 테스트라 평소 빌드에서 자동으로 돌아도 안전하다.
 */
@SpringBootTest
class PageControllerTest {

    @Autowired
    private PageController pageController;

    @Autowired
    private StandingsPageController standingsPageController;

    @Autowired
    private PlayerPageController playerPageController;

    @Test
    void 대시보드_페이지가_정상_렌더링된다() {
        Model model = new ExtendedModelMap();

        String view = pageController.schedule(null, model);

        assertThat(view).isEqualTo("dashboard");
        assertThat(model.containsAttribute("matches")).isTrue();
        assertThat(model.containsAttribute("dateTabs")).isTrue();
        assertThat(model.containsAttribute("selectedDate")).isTrue();
    }

    @Test
    void 순위표_페이지가_정상_렌더링된다() {
        Model model = new ExtendedModelMap();

        String view = standingsPageController.standings(null, model);

        assertThat(view).isEqualTo("standings");
        assertThat(model.containsAttribute("standings")).isTrue();
        assertThat(model.containsAttribute("season")).isTrue();
    }

    @Test
    void 선수_검색_페이지가_검색어_없이도_정상_렌더링된다() {
        Model model = new ExtendedModelMap();

        String view = playerPageController.players(null, model);

        assertThat(view).isEqualTo("players");
        assertThat(model.getAttribute("searched")).isEqualTo(false);
    }

    @Test
    void 선수_검색_페이지가_검색어와_함께_정상_렌더링된다() {
        Model model = new ExtendedModelMap();

        String view = playerPageController.players("김", model);

        assertThat(view).isEqualTo("players");
        assertThat(model.getAttribute("searched")).isEqualTo(true);
    }
}
