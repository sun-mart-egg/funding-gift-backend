INSERT INTO consumer
    (consumer_id, social_id, email, name, profile_image_url, phone_number, birthyear, birthday, gender, created_at, updated_at)
VALUES
    (1, '1', 'test@test.com', '아무개', 'test.jpg', '01033333333', '1997', '0509', 'male', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product
(product_id, name, price, description, image, review_avg, review_cnt, status, created_at, updated_at)
VALUES
    (1, '상품1', 50000, '테스트용 상품입니다.', 'product1.jpg', 0.0, 0, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_option
(product_option_id, name, price, status, product_id, created_at, updated_at)
VALUES
    (1, '상품1 옵션', 0, 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO anniversary_category
    (anniversary_category_id, name)
VALUES
    (1, '생일');
