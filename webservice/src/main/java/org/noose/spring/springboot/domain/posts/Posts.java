package org.noose.spring.springboot.domain.posts;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.noose.spring.springboot.domain.BaseTimeEntity;

import javax.persistence.*;

@Getter // Getter 메소드 자동 생성 (Lombok)
@NoArgsConstructor // 기본 생성자 자동 추가 (Lombok)
@Entity // 테이블과 링크될 클래스
public class Posts extends BaseTimeEntity {

    @Id // 해당 테이블의 PK 필드
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 생성 규칙
    private Long id;

    @Column(length = 500, nullable = false) // 테이블 컬럼
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String author;

    @Builder // 해당 클래스의 빌더 패턴 클래스를 생성 (Lombok) / 어느 필드에 어떤 값을 채워야 할지 명확하게 파악, 생성자와 다르다
    public Posts(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
