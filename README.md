<img src="https://s3.us-west-2.amazonaws.com/secure.notion-static.com/b63fbf3e-99f6-4f26-b379-e0cbc17b745f/%EC%83%81%EB%8B%A8_%ED%83%80%EC%9D%B4%ED%8B%80.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Credential=AKIAT73L2G45EIPT3X45%2F20220516%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Date=20220516T061012Z&X-Amz-Expires=86400&X-Amz-Signature=caf52f4902cc6aee34fc05eaf651c5bbe7a2a142ed56cbbee73ca83d3b473ee3&X-Amz-SignedHeaders=host&response-content-disposition=filename%20%3D%22%25EC%2583%2581%25EB%258B%25A8%2520%25ED%2583%2580%25EC%259D%25B4%25ED%258B%2580.JPG.jpg%22&x-id=GetObject" width="600">

## 📣 소개

### **남들과는 다른 독특한 활동을 추구하는 사람들을 위한 어메니티 기반의 커머스 어플리케이션**입니다.

어매니티 컨텐츠에는 예술(영화,미술, 음악), 운동(요가, 런닝, 수영), 라이프스타일(캠핑, 펫, 술, 타투, 플라워, 사진 등) 등이 있으며, 판매되는 어매니티 티켓은 지속적인 기획 및 체결되는 계약에 따라 상이합니다.

## 📋 주요 기술 스택 (Server)

- Java, Spring Boot, Spring Data JPA, MariaDB, Redis, Spring Security
- AWS(Route53, LoadBalancer, EC2, RDS, S3, CloudFront, Elasticache, CodeDeploy), Github Actions
- [API 명세](https://documenter.getpostman.com/view/17717982/UVyuTbGw)

## 📄 주요 개발 내용 (Server)

- 분산 락으로 주문 및 결제 트랜잭션 동시성 제어
- Redis로 재고 관리 및 남은 어매니티 티켓 수 조회 성능 최적화
- 응답 및 예외 처리 코드 개선 (CommonResponseMaker, CommonException, ResponseCode 클래스)

## ERD 
<img src="https://user-images.githubusercontent.com/71204049/168560323-e279bb88-4bca-41eb-ae9a-4ac85e21af27.png" width="800">
