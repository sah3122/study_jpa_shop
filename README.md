# 실전 스프링부트와 JPA 활용2 - API 개발과 성능 최적화 강의 정리

* JPA에서 DTO로 바로 조회
    * 일반적인 SQL을 사용할 때 처럼 원하는 값을 선택해서 조회
    * new 명령어를 사용해서 JPQL의 결과를 DTO로 즉시 변환
    * SELECT 절에서 원하는 데이터를 직접 선택하므로 DB -> 애플리케이션 네트웤 용량 최적화(생각보다 미비)
    * 리포지토리 재사용성 떨어짐, API 스펙에 맞춘 코드가 리포지토리에 들어가는 단점.
    

### 정리
    * 엔티티를  DTO로 변환 하거나 DTO로 바로 조회하는 두가지 방법은 각각 장단점이 있다. 둘중 상황에 따라서 더 나은 방법을 선택하면된다. <br>
    엔티티로 조회하면 리포지토리 재사용성도 좋고, 개발도 단순해진다. 따라서 권장하는 방법은 다음과 같다.
#### 쿼리 방식 선택 권장 순서
* 우선 엔티티를 DTO로 변환하는 방법을 선택
* 필요하면 패치 조인으로 성능을 최적화 한다. -> 대부분의 이슈가 해결된다.
* 그래도 안되면 DTO로 직접 조회하는 방법을 사용한다.
* 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template을 사용해서 SQL을 직접 사용한다. 
    
### Collection Fetch Join
* 패치 조인으로 SQL 실행 횟수를 줄일 수 있다.
* distinct 를 사용한 이유는 1 대 다 조인으로 인한 데이터 베이스 ROW가 증가 하므로 그 결과 엔티티의 조회수도 증가 하게 된다. <br>
JPA 의 distinct는 SQL에 distinct를 추가하고 더해서 같은 엔티티가 조회될 시 애플리케이션에서 중복을 걸러준다.
* 단점
    * 페이징 불가능
    * 컬렉션 패치 조인을 사용하면 페이징이 불가능하다. 하이버네이트는 경고 로그를 남기면서 모든 데이터를 DB에서 읽어 오고, 메모리에서 페이징 해버린다. (매우 위험하다).
    <br> 자세한 내용은 자바 ORM 표쥰 JPA 프로그래밍의 패치 조인 부분을 참고하자.
    * 컬렉션 패치 조인은 1개만 사용할 수 있다. 컬렉션 둘 이상에 패치 조인을 사용하면 안된다. 데이터가 부정합하게 조회될 수 있다. 
    자세한 내용은 자바 ORM 표준 JPA 프로그래밍을 참고하자.
### 페이징과 한계 돌파
* 컬렉션을 패치 조인 하면 페이징이 불가능하다.
    * 컬렉션을 패치 조인하면 일대다 조인이 발생하므로 데이터가 예측할 수 없이 증가한다.
    * 일대다에서 일을 기준으로 페이징을 하는것이 목적이다. 그런데 데이터는 다(N)을 기준으로 row가 생성된다.
    * Order를 기준으로 페이징을 하고 싶은데, 다(N)인 OrderItem을 조인하면 OrderItem이 기준이 되어 버린다.
* 이 경우 하이버네이트는 경고 로그를 남기고 모든 DB데이터를 읽어서 메모리에서 페이징을 시도한다.
* 한계 돌파
    * 먼저 XToOne(OneToOnem ManyToOne)관계를 모두 패치조인한다. ToOne관계는 row수를 증가시키지 않으므로 페이징 쿼리에 영향을 주지 않든다.
    * 컬렉션은 지연 로딩으로 조회한다.
    * 지연 로딩 성능 최적화를 위해 hibernate.default_batch_fetch_size, @BatchSize를 적용한다.
        * hibernate.default_batch_fetch_size : 글로벌 설정
        * @BatchSize : 개별 최적화
        * 이 옵션을 사용하면 컬렉션이나 프록시 객체를 한꺼번에 설정한 size만큼 IN 쿼리로 조회한다.
        * 개별로 설정하려면 @BatchSize를 적용하면 된다. (컬렉션은 컬렉션 필드에, 엔티티는 엔티티 클래스에 적용)
    * 장점 
        * 쿼리 호출 수가 1+ N -> 1 + 1로 최적화 된다.
        * 조인보다 DB 데이터 전송량이 최적화 된다. (Order 와 OrderItem을 조인하면 Order가 OrderItem만큼 중복해서 조회된다.)
        * 패치 조인 방식과 비교해서 쿼리 호출 수가 약간 증가하지만 DB 데이터 전송량이 감소한다.
        * 컬렉션 채피 조인은 페이징이 불가능하지만 이 방법은 페이징이 가능하다.
    * 결론 
        * ToOne관계는 패치 조인해도 페이징에 영향을 주지 않는다. 따라서 ToOne관계는 패치조인으로 쿼리 수를 줄이고 해결하고, 나머지는 hibernate.default_batch_fetch_size로 최적화 하자.
    * 참고
        * default_batch_fetch_size는 적당한 크기를 골라야 한다. (100 ~ 1000)
### JPA에서 DTO 직접 조회
* Query : 루트 1번, 컬렉션 N번 조회
* ToOne(N : 1, 1:1) 관계들은 먼저 조회 하고, ToMany(1:N) 관계는 각각 별도로 처리한다.
    * 이런 방식을 선택한 이유는 다음과 같다.
    * ToOne 관계는 조인해도 데이터 ROW 수가 증가하지 않는다.
    * ToMany(1:N) 관계는 조인하면 Row수가 증가한다.
* Row 수가 증가하지 않는 ToOne관계는 조인으로 최적화 하기 쉬우므로 한번에 조회하고, ToMany관계는 최적화 하기 어려우므로 FINDORDERITEMS 같은 별도의 베서드로 조회한다.