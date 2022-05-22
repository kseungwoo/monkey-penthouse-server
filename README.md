<img src="https://user-images.githubusercontent.com/71204049/169683697-052b2b01-118e-4719-94f3-125c329347f2.png" width="600">

## 📣 소개

### **남들과는 다른 독특한 활동을 추구하는 사람들을 위한 어메니티 기반의 커머스 어플리케이션**입니다.

어매니티 컨텐츠에는 예술(영화,미술, 음악), 운동(요가, 런닝, 수영), 라이프스타일(캠핑, 펫, 술, 타투, 플라워, 사진 등) 등이 있으며, 판매되는 어매니티 티켓은 지속적인 기획 및 체결되는 계약에 따라 상이합니다.

## 📋 주요 기술 스택 (Server)

- Java, Spring Boot, Spring Data JPA, MariaDB, Redis, Spring Security
- AWS(Route53, LoadBalancer, EC2, RDS, S3, CloudFront, Elasticache, CodeDeploy), Github Actions

## 📄 주요 개발 내용 (Server)

- 분산 락으로 주문 및 결제 트랜잭션 동시성 제어
- Redis로 재고 관리 및 남은 어매니티 티켓 수 조회 성능 최적화
- 응답 및 예외 처리 코드 개선 (CommonResponseMaker, CommonException, ResponseCode 클래스)

## 📝[API 명세](https://documenter.getpostman.com/view/17717982/UVyuTbGw)

## 🗺️ ERD 
<img src="https://user-images.githubusercontent.com/71204049/168560323-e279bb88-4bca-41eb-ae9a-4ac85e21af27.png" width="800">
