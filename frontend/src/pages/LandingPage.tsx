import { Link } from 'react-router-dom';
import { SectionCard } from '../components/SectionCard';

const coreValues = [
  ['금융 백엔드', '주문 상태머신, 계좌/현금 정산, DB 영속화를 하나의 흐름으로 보여줍니다.'],
  ['리스크 통제', '현금, 노출, 보유 수량, stale quote warning까지 주문 전후 통제 지점을 갖습니다.'],
  ['실시간 시세', 'polling 기반 quote 수집과 bid/ask 기반 체결 로직으로 운영감을 만듭니다.'],
  ['퀀트 리서치', '백테스트 artifact와 운영 주문 결과를 같은 제품 안에서 연결합니다.'],
];

const flow = [
  'quote 수집',
  'order 생성',
  'risk 평가',
  'execution',
  'position/cash 반영',
  'snapshot 생성',
  'research link',
];

export function LandingPage() {
  return (
    <div className="page-grid">
      <section className="hero-panel">
        <p>Quant Execution Risk Platform</p>
        <h1>시장데이터 기반 주문·리스크·체결·포트폴리오 플랫폼</h1>
        <p>
          QERP는 정량 전략 신호가 주문과 포트폴리오 성과로 이어지는 전체 경로를 한 번에 시연하기 위한
          포트폴리오 제품입니다.
        </p>
        <div className="hero-actions">
          <Link className="secondary-button" to="/console">
            운영 콘솔 보기
          </Link>
          <Link className="secondary-button" to="/architecture">
            아키텍처 보기
          </Link>
          <Link className="secondary-button" to="/research">
            리서치 보기
          </Link>
        </div>
      </section>

      <section className="two-col">
        {coreValues.map(([title, description]) => (
          <SectionCard key={title} title={title}>
            <p className="muted">{description}</p>
          </SectionCard>
        ))}
      </section>

      <SectionCard
        title="System Flow"
        subtitle="면접관이 1분 안에 구조를 이해할 수 있도록 핵심 흐름만 압축했습니다."
      >
        <div className="kpi-grid">
          {flow.map((item, index) => (
            <div className="kpi-card" key={item}>
              <span>STEP {index + 1}</span>
              <strong>{item}</strong>
            </div>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}
