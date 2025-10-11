-- 닉네임 컬럼 추가
ALTER TABLE users ADD COLUMN nickname VARCHAR(50);

-- 기존 데이터에 대한 닉네임 기본값 설정 (선택사항)
-- UPDATE users SET nickname = name WHERE nickname IS NULL;
